package com.nqd.nqd_lms_be.service.onehundredms;

import com.nqd.nqd_lms_be.config.onehundredms.OneHundredMsProperties;
import com.nqd.nqd_lms_be.dto.onehundredms.OneHundredMsRecordingAssetDto;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OneHundredMsSyncServiceImpl implements OneHundredMsSyncService {

    private final OneHundredMsService oneHundredMsService;
    private final OneHundredMsProperties oneHundredMsProperties;
    private final YouTubeUploadService youTubeUploadService;
    private final ClassroomRepository classroomRepository;
    private final ClassroomRecordedVideoRepository videoRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    @Override
    @Transactional
    public int syncRecordingsForClassroom(UUID classroomId) {
        if (!oneHundredMsService.isConfigured()) {
            log.warn("100ms service is not configured. Cannot sync recordings.");
            return 0;
        }

        Classroom classroom = classroomRepository.findById(classroomId).orElse(null);
        if (classroom == null || classroom.getMeetingId() == null || classroom.getMeetingId().isBlank()) {
            log.warn("Classroom or meetingId not found for id: {}", classroomId);
            return 0;
        }

        List<OneHundredMsRecordingAssetDto> assets = oneHundredMsService.getCompletedRecordingAssets(classroom.getMeetingId());
        if (assets.isEmpty()) {
            log.info("No completed recording assets found in 100ms for classroom '{}' (roomId: {}).", classroom.getName(), classroom.getMeetingId());
            return 0;
        }

        int syncedCount = 0;
        for (OneHundredMsRecordingAssetDto asset : assets) {
            if (processAndUploadAsset(classroom, asset)) {
                syncedCount++;
            }
        }

        log.info("Finished syncing 100ms recordings for classroom '{}': {} new videos processed.", classroom.getName(), syncedCount);
        return syncedCount;
    }

    @Override
    @Transactional
    public int syncAll100msRecordings() {
        if (!oneHundredMsService.isConfigured()) {
            return 0;
        }

        List<Classroom> classrooms = classroomRepository.findAll();
        int totalSynced = 0;
        for (Classroom classroom : classrooms) {
            if (classroom.getMeetingId() != null && !classroom.getMeetingId().isBlank()) {
                try {
                    totalSynced += syncRecordingsForClassroom(classroom.getId());
                } catch (Exception e) {
                    log.error("Failed to sync 100ms recordings for classroom {}: {}", classroom.getName(), e.getMessage());
                }
            }
        }
        return totalSynced;
    }

    @Scheduled(fixedDelay = 1800000, initialDelay = 180000)
    public void scheduledSync() {
        if (oneHundredMsService.isConfigured()) {
            try {
                log.info("Starting scheduled 100ms recording sync...");
                int count = syncAll100msRecordings();
                if (count > 0) {
                    log.info("Scheduled 100ms sync completed: {} new recordings saved.", count);
                }
            } catch (Exception e) {
                log.warn("Scheduled 100ms sync failed: {}", e.getMessage());
            }
        }
    }

    private boolean processAndUploadAsset(Classroom classroom, OneHundredMsRecordingAssetDto asset) {
        String title = "Buổi học " + classroom.getName() + " - Bản ghi " + (asset.getCreatedAt() != null ? asset.getCreatedAt().substring(0, 10) : "");
        String uniqueMarker = "100ms-" + asset.getId();

        // Check if already synced
        boolean exists = videoRepository.findByClassroomIdOrderBySessionDateDescCreatedAtDesc(classroom.getId())
                .stream()
                .anyMatch(v -> v.getTitle().contains(asset.getId()) || (v.getDescription() != null && v.getDescription().contains(uniqueMarker)));

        if (exists) {
            log.debug("Recording asset {} already synced for class {}. Skipping.", asset.getId(), classroom.getName());
            return false;
        }

        String downloadUrl = oneHundredMsService.getPresignedDownloadUrl(asset.getId());
        if (downloadUrl == null || downloadUrl.isBlank()) {
            log.warn("Could not get download URL for 100ms asset {}", asset.getId());
            return false;
        }

        String videoFinalUrl = null;
        int durationMinutes = asset.getDuration() != null && asset.getDuration() > 0 ? (int) (asset.getDuration() / 60) : 45;
        if (durationMinutes <= 0) durationMinutes = 30;

        String description = "Bản ghi buổi học trực tuyến 100ms Live Class lớp " + classroom.getName()
                + "\nMã bản ghi: " + uniqueMarker;

        // Attempt YouTube upload if configured
        try {
            HttpRequest downloadReq = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .GET()
                    .timeout(Duration.ofMinutes(30))
                    .build();

            HttpResponse<InputStream> resp = httpClient.send(downloadReq, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() == 200) {
                long contentLength = resp.headers().firstValueAsLong("Content-Length").orElse(-1L);
                try (InputStream is = resp.body()) {
                    List<String> tags = List.of("NQDLMS", "100ms", classroom.getName());
                    String yt = youTubeUploadService.uploadVideoStream(is, contentLength, title, description, tags);
                    if (yt != null && !yt.isBlank()) {
                        videoFinalUrl = yt;
                        log.info("Uploaded 100ms recording asset {} to YouTube: {}", asset.getId(), yt);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("YouTube upload failed for 100ms asset {}: {}. Falling back to 100ms URL.", asset.getId(), e.getMessage());
        }

        if (videoFinalUrl == null) {
            videoFinalUrl = downloadUrl;
        }

        LocalDate sessionDate = LocalDate.now();
        if (asset.getCreatedAt() != null) {
            try {
                sessionDate = OffsetDateTime.parse(asset.getCreatedAt()).toLocalDate();
            } catch (Exception ignored) {}
        }

        ClassroomRecordedVideo recordedVideo = ClassroomRecordedVideo.builder()
                .classroom(classroom)
                .title(title)
                .videoUrl(videoFinalUrl)
                .sessionDate(sessionDate)
                .durationMinutes(durationMinutes)
                .description(description)
                .uploadedBy(classroom.getTeacher())
                .build();

        videoRepository.save(recordedVideo);
        log.info("Saved 100ms recorded video '{}' for classroom '{}'", title, classroom.getName());
        return true;
    }
}
