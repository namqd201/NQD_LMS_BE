package com.nqd.nqd_lms_be.service.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.google.GoogleProperties;
import com.nqd.nqd_lms_be.entity.Classroom;
import com.nqd.nqd_lms_be.entity.ClassroomRecordedVideo;
import com.nqd.nqd_lms_be.repository.ClassroomRecordedVideoRepository;
import com.nqd.nqd_lms_be.repository.ClassroomRepository;
import com.nqd.nqd_lms_be.service.youtube.YouTubeUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDriveSyncServiceImpl implements GoogleDriveSyncService {

    private final GoogleOAuthService googleOAuthService;
    private final GoogleProperties googleProperties;
    private final YouTubeUploadService youTubeUploadService;
    private final ClassroomRepository classroomRepository;
    private final ClassroomRecordedVideoRepository videoRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    @Override
    @Transactional
    public int syncRecordingsForClassroom(UUID classroomId) {
        if (!googleOAuthService.isConfigured()) {
            log.warn("Google credentials not configured. Cannot sync Meet recordings.");
            return 0;
        }

        Classroom classroom = classroomRepository.findById(classroomId).orElse(null);
        if (classroom == null) {
            log.warn("Classroom not found for id: {}", classroomId);
            return 0;
        }

        List<DriveFileDto> files = fetchMeetRecordingsFromDrive();
        if (files.isEmpty()) {
            log.info("No MP4 recording files found in Google Drive.");
            return 0;
        }

        int syncedCount = 0;
        String normalizedClassName = classroom.getName().trim().toLowerCase();

        for (DriveFileDto file : files) {
            String lowerFileName = file.name.toLowerCase();

            // Match if filename contains classroom name OR if there's only 1 classroom OR contains class code
            boolean match = lowerFileName.contains(normalizedClassName)
                    || (classroom.getCode() != null && lowerFileName.contains(classroom.getCode().toLowerCase()));

            // If only one active classroom exists in the system, associate recording with it
            if (!match && classroomRepository.count() == 1) {
                match = true;
            }

            if (match) {
                if (processAndUploadVideo(classroom, file)) {
                    syncedCount++;
                }
            }
        }

        log.info("Finished syncing Google Drive recordings for classroom '{}': {} new videos uploaded.", classroom.getName(), syncedCount);
        return syncedCount;
    }

    @Override
    @Transactional
    public int syncAllMeetRecordings() {
        if (!googleOAuthService.isConfigured()) {
            return 0;
        }

        List<Classroom> classrooms = classroomRepository.findAll();
        if (classrooms.isEmpty()) {
            return 0;
        }

        List<DriveFileDto> files = fetchMeetRecordingsFromDrive();
        if (files.isEmpty()) {
            return 0;
        }

        int totalSynced = 0;
        for (Classroom classroom : classrooms) {
            String normalizedClassName = classroom.getName().trim().toLowerCase();
            for (DriveFileDto file : files) {
                String lowerFileName = file.name.toLowerCase();
                boolean match = lowerFileName.contains(normalizedClassName)
                        || (classroom.getCode() != null && lowerFileName.contains(classroom.getCode().toLowerCase()));

                if (match) {
                    if (processAndUploadVideo(classroom, file)) {
                        totalSynced++;
                    }
                }
            }
        }

        return totalSynced;
    }

    /**
     * Periodic auto-sync every 30 minutes to check if any class finished recording
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 120000)
    public void scheduledSync() {
        if (googleOAuthService.isConfigured() && googleProperties.getMeet().isEnabled()) {
            try {
                log.info("Starting scheduled Google Meet -> YouTube recording synchronization...");
                int synced = syncAllMeetRecordings();
                if (synced > 0) {
                    log.info("Scheduled sync completed: uploaded {} new recordings to YouTube.", synced);
                }
            } catch (Exception e) {
                log.warn("Scheduled sync failed: {}", e.getMessage());
            }
        }
    }

    private boolean processAndUploadVideo(Classroom classroom, DriveFileDto file) {
        String title = stripFileExtension(file.name);
        if (videoRepository.existsByClassroomIdAndTitle(classroom.getId(), title)) {
            log.debug("Video already exists for class {} with title '{}'. Skipping.", classroom.getName(), title);
            return false;
        }

        String accessToken = googleOAuthService.getFreshAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }

        try {
            log.info("Downloading recording '{}' (size: {} bytes) from Google Drive...", file.name, file.size);

            HttpRequest downloadRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/drive/v3/files/" + file.id + "?alt=media"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .timeout(Duration.ofMinutes(30))
                    .build();

            HttpResponse<InputStream> downloadResponse = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (downloadResponse.statusCode() != 200) {
                log.error("Failed to download file {} from Google Drive: status={}", file.id, downloadResponse.statusCode());
                return false;
            }

            try (InputStream is = downloadResponse.body()) {
                String desc = "Bản ghi buổi học trực tuyến Google Meet lớp " + classroom.getName() + " - Hệ thống NQD-LMS";
                List<String> tags = List.of("NQDLMS", "GoogleMeet", classroom.getName());

                String youtubeUrl = youTubeUploadService.uploadVideoStream(is, file.size, title, desc, tags);
                if (youtubeUrl != null && !youtubeUrl.isBlank()) {
                    LocalDate sessionDate = LocalDate.now();
                    if (file.createdTime != null) {
                        try {
                            sessionDate = OffsetDateTime.parse(file.createdTime).toLocalDate();
                        } catch (Exception ignored) {}
                    }

                    // Estimate duration in minutes based on file size (~2MB per minute for 720p/1080p recording)
                    int estimatedMinutes = (int) Math.max(15, Math.min(240, file.size / (1024 * 1024 * 2)));

                    ClassroomRecordedVideo recordedVideo = ClassroomRecordedVideo.builder()
                            .classroom(classroom)
                            .title(title)
                            .videoUrl(youtubeUrl)
                            .sessionDate(sessionDate)
                            .durationMinutes(estimatedMinutes)
                            .description(desc)
                            .uploadedBy(classroom.getTeacher())
                            .build();

                    videoRepository.save(recordedVideo);
                    log.info("Saved recorded video to LMS database: title='{}', url='{}'", title, youtubeUrl);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Error processing recording file {} for classroom {}", file.name, classroom.getName(), e);
        }

        return false;
    }

    private List<DriveFileDto> fetchMeetRecordingsFromDrive() {
        List<DriveFileDto> result = new ArrayList<>();
        String accessToken = googleOAuthService.getFreshAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            return result;
        }

        try {
            // Query for MP4 video files in Drive, ordered by most recently created
            String query = "mimeType = 'video/mp4' and trashed = false";
            String url = "https://www.googleapis.com/drive/v3/files?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&fields=files(id,name,createdTime,size,webViewLink)&orderBy=createdTime%20desc&pageSize=50";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode filesNode = root.path("files");
                if (filesNode.isArray()) {
                    for (JsonNode f : filesNode) {
                        DriveFileDto dto = new DriveFileDto();
                        dto.id = f.path("id").asText();
                        dto.name = f.path("name").asText();
                        dto.createdTime = f.path("createdTime").asText(null);
                        dto.size = f.path("size").asLong(0);
                        dto.webViewLink = f.path("webViewLink").asText(null);
                        result.add(dto);
                    }
                }
            } else {
                log.error("Failed to query Google Drive files: status={}, body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Exception fetching Google Drive files", e);
        }

        return result;
    }

    private String stripFileExtension(String fileName) {
        if (fileName == null) return "Bản ghi buổi học";
        int idx = fileName.lastIndexOf('.');
        return (idx > 0) ? fileName.substring(0, idx) : fileName;
    }

    private static class DriveFileDto {
        String id;
        String name;
        String createdTime;
        long size;
        String webViewLink;
    }
}
