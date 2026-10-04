package com.nqd.nqd_lms_be.dto.lab;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabRecordedVideoResponse {
    private UUID id;
    private UUID labRoomId;
    private String labRoomTitle;
    private String title;
    private String videoUrl;
    private Integer durationMinutes;
    private LocalDate recordedDate;
    private String description;
    private UUID ownerUserId;
    private String ownerName;
    private LocalDateTime createdAt;
}
