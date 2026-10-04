package com.nqd.nqd_lms_be.dto.lab;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabRoomResponse {
    private UUID id;
    private String title;
    private String description;
    private UUID hostUserId;
    private String hostName;
    private String hostAvatarUrl;
    private String speakerName;
    private String speakerTitle;
    private String coverImageUrl;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private Integer estimatedDurationMinutes;
    private String status;
    private Boolean isPublic;
    private String guestMeetingUrl;
    private String hostMeetingUrl;
    private Boolean isHostOrAdmin;
    private Integer maxParticipants;
    private LocalDateTime createdAt;
}
