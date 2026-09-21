package com.nqd.nqd_lms_be.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private String type;
    private String title;
    private String body;
    private String linkUrl;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
