package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.LabStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "lab_rooms",
    indexes = {
        @Index(name = "idx_lab_rooms_status_start", columnList = "status, scheduled_start_time"),
        @Index(name = "idx_lab_rooms_host", columnList = "host_user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class LabRoom extends BaseEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User hostUser;

    @Column(name = "speaker_name", nullable = false, length = 255)
    private String speakerName;

    @Column(name = "speaker_title", length = 100)
    private String speakerTitle; // "Giáo sư", "Tiến sĩ", "Thạc sĩ", "Giảng viên"...

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Column(name = "scheduled_start_time", nullable = false)
    private LocalDateTime scheduledStartTime;

    @Column(name = "scheduled_end_time")
    private LocalDateTime scheduledEndTime;

    @Column(name = "estimated_duration_minutes")
    @Builder.Default
    private Integer estimatedDurationMinutes = 60;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private LabStatus status = LabStatus.SCHEDULED;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "meeting_room_id", length = 100)
    private String meetingRoomId;

    @Column(name = "host_meeting_url", columnDefinition = "TEXT")
    private String hostMeetingUrl;

    @Column(name = "guest_meeting_url", columnDefinition = "TEXT")
    private String guestMeetingUrl;

    @Column(name = "meeting_passcode", length = 100)
    private String meetingPasscode;

    @Column(name = "max_participants")
    @Builder.Default
    private Integer maxParticipants = 500;
}
