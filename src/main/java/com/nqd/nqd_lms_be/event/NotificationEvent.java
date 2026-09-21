package com.nqd.nqd_lms_be.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {
    private UUID userId;
    private String type;
    private String title;
    private String body;
    private String linkUrl;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
