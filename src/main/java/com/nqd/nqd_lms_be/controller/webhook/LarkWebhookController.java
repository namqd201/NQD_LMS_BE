package com.nqd.nqd_lms_be.controller.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.dto.lark.LarkRecordingDto;
import com.nqd.nqd_lms_be.entity.Classroom;
import com.nqd.nqd_lms_be.entity.ClassroomRecordedVideo;
import com.nqd.nqd_lms_be.repository.ClassroomRecordedVideoRepository;
import com.nqd.nqd_lms_be.repository.ClassroomRepository;
import com.nqd.nqd_lms_be.service.lark.LarkService;
import com.nqd.nqd_lms_be.service.r2.R2StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/lark")
@RequiredArgsConstructor
@Tag(name = "Lark Webhook", description = "Endpoints for handling Lark events (Meeting recording ready, URL challenge)")
public class LarkWebhookController {

    private final LarkService larkService;
    private final R2StorageService r2StorageService;
    private final com.nqd.nqd_lms_be.service.youtube.YouTubeUploadService youTubeUploadService;
    private final ClassroomRepository classroomRepository;
    private final ClassroomRecordedVideoRepository videoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    @Operation(summary = "Receive and handle Lark Open Platform event webhooks")
    public ResponseEntity<?> handleLarkWebhook(@RequestBody String rawPayload) {
        log.info("Received Lark Webhook payload: {}", rawPayload);

        try {
            JsonNode root = objectMapper.readTree(rawPayload);

            // 1. Handle Lark URL Verification Challenge
            if (root.has("challenge")) {
                String challenge = root.path("challenge").asText();
                log.info("Responding to Lark URL verification challenge: {}", challenge);
                return ResponseEntity.ok(Map.of("challenge", challenge));
            }

            // 2. Handle Event Notification (v2 schema)
            String eventType = root.path("header").path("event_type").asText();
            if ("vc.meeting.recording_ready_v1".equalsIgnoreCase(eventType)) {
                String meetingId = root.path("event").path("meeting").path("id").asText();
                String topic = root.path("event").path("meeting").path("topic").asText();
                log.info("Processing vc.meeting.recording_ready_v1 for meetingId={}, topic={}", meetingId, topic);

                // Asynchronously or inline process the recording
                processRecording(meetingId, topic);
            }

            return ResponseEntity.ok(Map.of("code", 0, "msg", "success"));
        } catch (Exception e) {
            log.error("Error processing Lark webhook", e);
            return ResponseEntity.ok(Map.of("code", 0, "msg", "handled with error"));
        }
    }

    private void processRecording(String meetingId, String topic) {
        if (meetingId == null || meetingId.isBlank()) return;

        Optional<Classroom> classroomOpt = classroomRepository.findFirstByMeetingId(meetingId)
                .or(() -> classroomRepository.findFirstByLarkMeetingUrlContaining(meetingId));

        if (classroomOpt.isEmpty()) {
            log.warn("No classroom found matching meetingId: {}. Skipping auto-sync.", meetingId);
            return;
        }

        Classroom classroom = classroomOpt.get();
        log.info("Found classroom '{}' (ID: {}) for meetingId: {}", classroom.getName(), classroom.getId(), meetingId);

        // Fetch download URL from Lark
        LarkRecordingDto recording = larkService.getMeetingRecording(meetingId);
        if (recording == null || recording.getUrl() == null || recording.getUrl().isBlank()) {
            log.warn("Could not retrieve recording download URL from Lark for meetingId: {}", meetingId);
            return;
        }

        String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String videoTitle = "[" + classroom.getName() + "] - "
                + ((topic != null && !topic.isBlank()) ? topic : "Buổi học online ngày " + formattedDate);
        String videoDescription = "Video bài giảng trực tuyến lớp học " + classroom.getName()
                + " trên hệ thống NQD-LMS. Ghi hình tự động qua Lark Suite.";

        String finalVideoUrl = null;
        String storageProvider = "Lark Suite";

        // 1. First priority: Stream upload to YouTube (Unlisted)
        if (youTubeUploadService != null && youTubeUploadService.isConfigured()) {
            log.info("Starting automated upload to YouTube for classroom '{}'...", classroom.getName());
            String ytUrl = youTubeUploadService.uploadVideoFromUrl(
                    recording.getUrl(),
                    videoTitle,
                    videoDescription,
                    java.util.List.of("NQD-LMS", "HocOnline", classroom.getName())
            );
            if (ytUrl != null && !ytUrl.isBlank()) {
                finalVideoUrl = ytUrl;
                storageProvider = "YouTube (Không công khai)";
                log.info("Video successfully uploaded to YouTube: {}", finalVideoUrl);
            }
        }

        // 2. Second priority: Fallback to Cloudflare R2 if YouTube upload did not run/succeed
        if ((finalVideoUrl == null || finalVideoUrl.isBlank()) && r2StorageService != null) {
            String r2Key = "recordings/" + classroom.getId() + "/" + meetingId + "_" + System.currentTimeMillis() + ".mp4";
            String r2Url = r2StorageService.uploadFromUrl(recording.getUrl(), r2Key, "video/mp4");
            if (r2Url != null && !r2Url.isBlank() && !r2Url.equals(recording.getUrl())) {
                finalVideoUrl = r2Url;
                storageProvider = "Cloudflare R2";
            }
        }

        // 3. Fallback to Lark download URL if both external storages are unavailable
        if (finalVideoUrl == null || finalVideoUrl.isBlank()) {
            finalVideoUrl = recording.getUrl();
            log.warn("External storage upload was not completed. Preserving original Lark URL.");
        } else {
            // Once securely in YouTube or R2, delete original video from Lark to save 100GB Lark storage
            log.info("Video securely stored in {}. Deleting original video from Lark to free storage...", storageProvider);
            larkService.deleteMeetingRecording(meetingId);
        }

        // Save recorded video entry into database
        int durationMinutes = recording.getDurationMs() != null ? (int) (recording.getDurationMs() / 60000L) : 0;

        ClassroomRecordedVideo recordedVideo = ClassroomRecordedVideo.builder()
                .classroom(classroom)
                .title(videoTitle)
                .videoUrl(finalVideoUrl)
                .sessionDate(LocalDate.now())
                .durationMinutes(durationMinutes > 0 ? durationMinutes : 60)
                .description("Bản ghi buổi học trực tuyến qua Lark được tự động lưu trữ trên " + storageProvider + ".")
                .uploadedBy(classroom.getTeacher())
                .build();

        videoRepository.save(recordedVideo);
        log.info("Saved recorded video entry ID: {} for classroom '{}' with URL: {}", recordedVideo.getId(), classroom.getName(), finalVideoUrl);
    }
}
