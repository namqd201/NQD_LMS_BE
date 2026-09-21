package com.nqd.nqd_lms_be.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadNotificationCountResponse {
    private long unreadCount;
}
