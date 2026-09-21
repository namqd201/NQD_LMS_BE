package com.nqd.nqd_lms_be.service.notification;

import com.nqd.nqd_lms_be.dto.notification.NotificationResponse;
import com.nqd.nqd_lms_be.dto.notification.UnreadNotificationCountResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    List<NotificationResponse> getUserNotifications(UUID userId);
    UnreadNotificationCountResponse getUnreadCount(UUID userId);
    void markAsRead(UUID notificationId, UUID userId);
    void markAllAsRead(UUID userId);
}
