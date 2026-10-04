package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDate;

@Entity
@Table(
    name = "lab_recorded_videos",
    indexes = {
        @Index(name = "idx_lab_videos_owner", columnList = "owner_user_id"),
        @Index(name = "idx_lab_videos_room", columnList = "lab_room_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class LabRecordedVideo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_room_id", nullable = false)
    private LabRoom labRoom;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "video_url", nullable = false, columnDefinition = "TEXT")
    private String videoUrl;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "recorded_date")
    private LocalDate recordedDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @Column(name = "recording_asset_id", length = 100)
    private String recordingAssetId;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
}
