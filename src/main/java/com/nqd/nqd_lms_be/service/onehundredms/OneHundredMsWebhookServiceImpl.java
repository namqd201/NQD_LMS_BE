package com.nqd.nqd_lms_be.service.onehundredms;

import com.fasterxml.jackson.databind.JsonNode;
import com.nqd.nqd_lms_be.entity.enums.LabStatus;
import com.nqd.nqd_lms_be.repository.ClassroomRepository;
import com.nqd.nqd_lms_be.repository.LabRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OneHundredMsWebhookServiceImpl implements OneHundredMsWebhookService {

    private final ClassroomRepository classroomRepository;
    private final LabRoomRepository labRoomRepository;
    private final OneHundredMsSyncService oneHundredMsSyncService;

    @Override
    @Transactional
    public void handleWebhook(JsonNode payload) {
        if (payload == null || payload.isNull()) return;

        String type = payload.path("type").asText(payload.path("event_type").asText(""));
        JsonNode data = payload.path("data");
        String roomId = data.path("room_id").asText(payload.path("room_id").asText(""));

        log.info("Received 100ms webhook: type={}, roomId={}", type, roomId);

        if (roomId.isBlank()) {
            log.info("100ms webhook received with no room_id (test ping event). Successfully acknowledged.");
            return;
        }

        switch (type) {
            case "session.open.success":
                handleSessionOpen(roomId, data);
                break;
            case "session.close.success":
                handleSessionClose(roomId, data);
                break;
            case "peer.join.success":
                handlePeerJoin(roomId, data);
                break;
            case "peer.leave.success":
                handlePeerLeave(roomId, data);
                break;
            case "recording.success":
            case "beam.recording.success":
                handleRecordingSuccess(roomId, data);
                break;
            default:
                log.info("100ms webhook unhandled event type: {}", type);
                break;
        }
    }

    private void handleSessionOpen(String roomId, JsonNode data) {
        // 1. Mark Classroom as LIVE
        classroomRepository.findFirstByMeetingId(roomId).ifPresent(c -> {
            c.setIsLiveNow(true);
            classroomRepository.save(c);
            log.info("Classroom '{}' ({}) is now marked LIVE from 100ms session.open.success", c.getName(), c.getId());
        });

        // 2. Mark LabRoom as LIVE
        labRoomRepository.findByMeetingRoomIdAndIsDeletedFalse(roomId).ifPresent(lab -> {
            lab.setStatus(LabStatus.LIVE);
            labRoomRepository.save(lab);
            log.info("LabRoom '{}' ({}) status changed to LIVE from 100ms session.open.success", lab.getTitle(), lab.getId());
        });
    }

    private void handleSessionClose(String roomId, JsonNode data) {
        // 1. Mark Classroom as NOT LIVE
        classroomRepository.findFirstByMeetingId(roomId).ifPresent(c -> {
            c.setIsLiveNow(false);
            classroomRepository.save(c);
            log.info("Classroom '{}' ({}) LIVE session ENDED from 100ms session.close.success", c.getName(), c.getId());

            // Auto-trigger sync recordings for classroom
            try {
                oneHundredMsSyncService.syncRecordingsForClassroom(c.getId());
            } catch (Exception e) {
                log.warn("Auto-sync recordings after session close failed for classroom {}: {}", c.getId(), e.getMessage());
            }
        });

        // 2. Mark LabRoom as ENDED
        labRoomRepository.findByMeetingRoomIdAndIsDeletedFalse(roomId).ifPresent(lab -> {
            lab.setStatus(LabStatus.ENDED);
            labRoomRepository.save(lab);
            log.info("LabRoom '{}' ({}) status changed to ENDED from 100ms session.close.success", lab.getTitle(), lab.getId());
        });
    }

    private void handlePeerJoin(String roomId, JsonNode data) {
        String userName = data.path("user_name").asText(data.path("name").asText(""));
        String role = data.path("role").asText("");
        String userId = data.path("user_id").asText("");

        log.info("100ms Peer joined room {}: name='{}', role='{}', userId='{}'", roomId, userName, role, userId);

        // Ensure Classroom is LIVE if any peer joins
        classroomRepository.findFirstByMeetingId(roomId).ifPresent(c -> {
            if (!Boolean.TRUE.equals(c.getIsLiveNow())) {
                c.setIsLiveNow(true);
                classroomRepository.save(c);
                log.info("Classroom '{}' marked LIVE due to peer join", c.getName());
            }
        });

        // Ensure LabRoom is LIVE if scheduled
        labRoomRepository.findByMeetingRoomIdAndIsDeletedFalse(roomId).ifPresent(lab -> {
            if (lab.getStatus() == LabStatus.SCHEDULED) {
                lab.setStatus(LabStatus.LIVE);
                labRoomRepository.save(lab);
                log.info("LabRoom '{}' marked LIVE due to peer join", lab.getTitle());
            }
        });
    }

    private void handlePeerLeave(String roomId, JsonNode data) {
        String userName = data.path("user_name").asText(data.path("name").asText(""));
        long duration = data.path("duration").asLong(0);
        log.info("100ms Peer left room {}: name='{}', duration={} seconds", roomId, userName, duration);
    }

    private void handleRecordingSuccess(String roomId, JsonNode data) {
        log.info("100ms recording ready webhook for room: {}", roomId);

        classroomRepository.findFirstByMeetingId(roomId).ifPresent(c -> {
            try {
                int synced = oneHundredMsSyncService.syncRecordingsForClassroom(c.getId());
                log.info("Auto-synced {} recording(s) for classroom '{}' ({}) via webhook", synced, c.getName(), c.getId());
            } catch (Exception e) {
                log.error("Failed to auto-sync recordings for classroom {} from webhook: {}", c.getId(), e.getMessage());
            }
        });
    }
}
