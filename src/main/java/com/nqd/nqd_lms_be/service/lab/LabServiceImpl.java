package com.nqd.nqd_lms_be.service.lab;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.lab.CreateLabRoomRequest;
import com.nqd.nqd_lms_be.dto.lab.CreateLabVideoRequest;
import com.nqd.nqd_lms_be.dto.lab.LabRecordedVideoResponse;
import com.nqd.nqd_lms_be.dto.lab.LabRoomResponse;
import com.nqd.nqd_lms_be.dto.onehundredms.OneHundredMsRecordingAssetDto;
import com.nqd.nqd_lms_be.dto.onehundredms.OneHundredMsRoomDto;
import com.nqd.nqd_lms_be.entity.LabRecordedVideo;
import com.nqd.nqd_lms_be.entity.LabRoom;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.LabStatus;
import com.nqd.nqd_lms_be.repository.LabRecordedVideoRepository;
import com.nqd.nqd_lms_be.repository.LabRoomRepository;
import com.nqd.nqd_lms_be.repository.UserRepository;
import com.nqd.nqd_lms_be.service.onehundredms.OneHundredMsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabServiceImpl implements LabService {

    private final LabRoomRepository labRoomRepository;
    private final LabRecordedVideoRepository recordedVideoRepository;
    private final UserRepository userRepository;
    private final OneHundredMsService oneHundredMsService;

    @Override
    @Transactional
    public LabRoomResponse createLabRoom(UUID hostUserId, CreateLabRoomRequest request) {
        User host = userRepository.findById(hostUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", hostUserId));

        LocalDateTime startTime = request.getScheduledStartTime();
        int duration = request.getEstimatedDurationMinutes() != null ? request.getEstimatedDurationMinutes() : 60;
        LocalDateTime endTime = startTime.plusMinutes(duration);

        String roomId = null;
        String hostUrl = null;
        String guestUrl = null;
        String passcode = null;

        // Automatically provision 100ms room
        if (oneHundredMsService != null && oneHundredMsService.isConfigured()) {
            try {
                OneHundredMsRoomDto roomDto = oneHundredMsService.createClassroomMeeting(
                        "lab-" + request.getTitle(),
                        "Phòng Lab trực tuyến: " + request.getTitle()
                );
                if (roomDto != null) {
                    roomId = roomDto.getRoomId();
                    hostUrl = roomDto.getHostMeetingUrl();
                    guestUrl = roomDto.getGuestMeetingUrl();
                    passcode = roomDto.getHostCode();
                }
            } catch (Exception e) {
                log.error("Failed to provision 100ms room for Lab {}", request.getTitle(), e);
            }
        }

        // Fallback meeting link if 100ms is not configured or in dev
        if (guestUrl == null) {
            String fakeCode = "lab-" + UUID.randomUUID().toString().substring(0, 8);
            guestUrl = "https://app.100ms.live/meeting/" + fakeCode;
            hostUrl = guestUrl + "?role=host";
            roomId = fakeCode;
            passcode = fakeCode.substring(0, 6);
        }

        LabRoom room = LabRoom.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .hostUser(host)
                .speakerName(request.getSpeakerName())
                .speakerTitle(request.getSpeakerTitle() != null && !request.getSpeakerTitle().isBlank() ? request.getSpeakerTitle() : "Giảng viên")
                .coverImageUrl(request.getCoverImageUrl())
                .scheduledStartTime(startTime)
                .scheduledEndTime(endTime)
                .estimatedDurationMinutes(duration)
                .status(LabStatus.SCHEDULED)
                .isPublic(true)
                .meetingRoomId(roomId)
                .hostMeetingUrl(hostUrl)
                .guestMeetingUrl(guestUrl)
                .meetingPasscode(passcode)
                .maxParticipants(500)
                .build();

        room = labRoomRepository.save(room);
        log.info("Created Lab room {} by host {}", room.getId(), host.getFullName());

        return mapToResponse(room, hostUserId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabRoomResponse> getPublicLabRooms(UUID currentUserId, boolean isAdmin) {
        List<LabRoom> rooms = labRoomRepository.findAllActiveRooms();
        return rooms.stream()
                .map(r -> mapToResponse(r, currentUserId, isAdmin))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LabRoomResponse getLabRoomById(UUID labId, UUID currentUserId, boolean isAdmin) {
        LabRoom room = labRoomRepository.findById(labId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("LabRoom", labId));

        return mapToResponse(room, currentUserId, isAdmin);
    }

    @Override
    @Transactional
    public LabRoomResponse updateLabStatus(UUID labId, UUID currentUserId, boolean isAdmin, LabStatus status) {
        LabRoom room = labRoomRepository.findById(labId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("LabRoom", labId));

        boolean isHost = room.getHostUser().getId().equals(currentUserId);
        if (!isHost && !isAdmin) {
            throw new ForbiddenOperationException("Chỉ chủ phòng Lab hoặc Quản trị viên mới có quyền cập nhật trạng thái phòng.");
        }

        room.setStatus(status);
        if (status == LabStatus.ENDED) {
            room.setScheduledEndTime(LocalDateTime.now());
        }
        room = labRoomRepository.save(room);

        return mapToResponse(room, currentUserId, isAdmin);
    }

    @Override
    @Transactional
    public void deleteLabRoom(UUID labId, UUID currentUserId, boolean isAdmin) {
        LabRoom room = labRoomRepository.findById(labId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("LabRoom", labId));

        boolean isHost = room.getHostUser().getId().equals(currentUserId);
        if (!isHost && !isAdmin) {
            throw new ForbiddenOperationException("Chỉ chủ phòng Lab hoặc Quản trị viên mới có quyền xóa phòng.");
        }

        room.setIsDeleted(true);
        room.setDeletedAt(LocalDateTime.now());
        labRoomRepository.save(room);

        // Optionally disable 100ms meeting
        if (oneHundredMsService != null && room.getMeetingRoomId() != null) {
            try {
                oneHundredMsService.disableMeeting(room.getMeetingRoomId());
            } catch (Exception e) {
                log.warn("Failed to disable 100ms meeting room {}", room.getMeetingRoomId(), e);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabRecordedVideoResponse> getRecordedVideos(UUID currentUserId, boolean isAdmin) {
        // STRICT PRIVACY: Only Admin and the Host/Channel Owner can see recorded videos!
        if (currentUserId == null) {
            return Collections.emptyList();
        }

        List<LabRecordedVideo> videos = isAdmin
                ? recordedVideoRepository.findAllForAdmin()
                : recordedVideoRepository.findByOwnerUserId(currentUserId);

        return videos.stream().map(this::mapToVideoResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LabRecordedVideoResponse saveRecordedVideo(UUID labId, UUID currentUserId, boolean isAdmin, CreateLabVideoRequest request) {
        LabRoom room = labRoomRepository.findById(labId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("LabRoom", labId));

        boolean isHost = room.getHostUser().getId().equals(currentUserId);
        if (!isHost && !isAdmin) {
            throw new ForbiddenOperationException("Chỉ chủ phòng Lab hoặc Quản trị viên mới có quyền lưu video bản ghi.");
        }

        User owner = room.getHostUser();

        LabRecordedVideo video = LabRecordedVideo.builder()
                .labRoom(room)
                .title(request.getTitle())
                .videoUrl(request.getVideoUrl())
                .durationMinutes(request.getDurationMinutes())
                .recordedDate(request.getRecordedDate() != null ? request.getRecordedDate() : LocalDate.now())
                .description(request.getDescription())
                .ownerUser(owner)
                .build();

        video = recordedVideoRepository.save(video);
        log.info("Saved recorded video for Lab {}: videoId={}", labId, video.getId());

        return mapToVideoResponse(video);
    }

    @Override
    @Transactional
    public int sync100msRecordings(UUID labId, UUID currentUserId, boolean isAdmin) {
        LabRoom room = labRoomRepository.findById(labId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("LabRoom", labId));

        boolean isHost = room.getHostUser().getId().equals(currentUserId);
        if (!isHost && !isAdmin) {
            throw new ForbiddenOperationException("Chỉ chủ phòng Lab hoặc Quản trị viên mới có quyền đồng bộ video.");
        }

        if (oneHundredMsService == null || !oneHundredMsService.isConfigured() || room.getMeetingRoomId() == null) {
            return 0;
        }

        try {
            List<OneHundredMsRecordingAssetDto> assets = oneHundredMsService.getCompletedRecordingAssets(room.getMeetingRoomId());
            int count = 0;

            for (OneHundredMsRecordingAssetDto asset : assets) {
                if (asset.getId() == null || recordedVideoRepository.existsByRecordingAssetId(asset.getId())) {
                    continue;
                }

                String downloadUrl = oneHundredMsService.getPresignedDownloadUrl(asset.getId());
                if (downloadUrl == null || downloadUrl.isBlank()) {
                    continue;
                }

                int durationMinutes = asset.getDuration() != null ? (int) (asset.getDuration() / 60) : 0;
                LabRecordedVideo video = LabRecordedVideo.builder()
                        .labRoom(room)
                        .title("Bản ghi Phòng Lab: " + room.getTitle() + " (" + (durationMinutes > 0 ? durationMinutes + " phút" : "Video") + ")")
                        .videoUrl(downloadUrl)
                        .durationMinutes(durationMinutes)
                        .recordedDate(LocalDate.now())
                        .description("Tự động đồng bộ từ phòng video call 100ms của buổi Lab.")
                        .ownerUser(room.getHostUser())
                        .recordingAssetId(asset.getId())
                        .build();

                recordedVideoRepository.save(video);
                count++;
            }

            return count;
        } catch (Exception e) {
            log.error("Failed to sync 100ms recordings for Lab room {}", labId, e);
            return 0;
        }
    }

    @Override
    @Transactional
    public void deleteRecordedVideo(UUID videoId, UUID currentUserId, boolean isAdmin) {
        LabRecordedVideo video = recordedVideoRepository.findById(videoId)
                .filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("LabRecordedVideo", videoId));

        boolean isOwner = video.getOwnerUser().getId().equals(currentUserId);
        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException("Chỉ chủ kênh hoặc Quản trị viên mới có quyền xóa video bản ghi này.");
        }

        video.setIsDeleted(true);
        video.setDeletedAt(LocalDateTime.now());
        recordedVideoRepository.save(video);
    }

    // --- Helpers ---

    private LabRoomResponse mapToResponse(LabRoom room, UUID currentUserId, boolean isAdmin) {
        boolean isHost = currentUserId != null && room.getHostUser().getId().equals(currentUserId);
        boolean canSeeHostUrl = isHost || isAdmin;

        return LabRoomResponse.builder()
                .id(room.getId())
                .title(room.getTitle())
                .description(room.getDescription())
                .hostUserId(room.getHostUser().getId())
                .hostName(room.getHostUser().getFullName())
                .hostAvatarUrl(room.getHostUser().getAvatarUrl())
                .speakerName(room.getSpeakerName())
                .speakerTitle(room.getSpeakerTitle())
                .coverImageUrl(room.getCoverImageUrl())
                .scheduledStartTime(room.getScheduledStartTime())
                .scheduledEndTime(room.getScheduledEndTime())
                .estimatedDurationMinutes(room.getEstimatedDurationMinutes())
                .status(room.getStatus().name())
                .isPublic(room.getIsPublic())
                .guestMeetingUrl(room.getGuestMeetingUrl())
                .hostMeetingUrl(canSeeHostUrl ? room.getHostMeetingUrl() : null)
                .isHostOrAdmin(canSeeHostUrl)
                .maxParticipants(room.getMaxParticipants())
                .createdAt(room.getCreatedAt())
                .build();
    }

    private LabRecordedVideoResponse mapToVideoResponse(LabRecordedVideo v) {
        return LabRecordedVideoResponse.builder()
                .id(v.getId())
                .labRoomId(v.getLabRoom().getId())
                .labRoomTitle(v.getLabRoom().getTitle())
                .title(v.getTitle())
                .videoUrl(v.getVideoUrl())
                .durationMinutes(v.getDurationMinutes())
                .recordedDate(v.getRecordedDate())
                .description(v.getDescription())
                .ownerUserId(v.getOwnerUser().getId())
                .ownerName(v.getOwnerUser().getFullName())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
