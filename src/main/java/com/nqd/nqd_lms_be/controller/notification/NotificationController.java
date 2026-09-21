package com.nqd.nqd_lms_be.controller.notification;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.notification.NotificationResponse;
import com.nqd.nqd_lms_be.dto.notification.UnreadNotificationCountResponse;
import com.nqd.nqd_lms_be.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Notifications", description = "Endpoints for viewing and managing user notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications for currently logged in user")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(notificationService.getUserNotifications(principal.getId()));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get count of unread notifications for bell badge")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(notificationService.getUnreadCount(principal.getId()));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<MessageResponse> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Notification marked as read"));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read for current user")
    public ResponseEntity<MessageResponse> markAllAsRead(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(MessageResponse.of("All notifications marked as read"));
    }
}
