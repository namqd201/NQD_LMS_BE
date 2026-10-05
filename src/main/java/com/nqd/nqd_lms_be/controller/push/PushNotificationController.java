package com.nqd.nqd_lms_be.controller.push;

import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.push.PushSubscriptionDto;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.repository.UserRepository;
import com.nqd.nqd_lms_be.service.push.WebPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
@Tag(name = "Push Notifications", description = "Endpoints for managing Web Push subscriptions and VAPID keys")
public class PushNotificationController {

    private final WebPushService webPushService;
    private final UserRepository userRepository;

    @GetMapping("/public-key")
    @Operation(summary = "Get VAPID public key for browser push subscription")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", webPushService.getVapidPublicKey()));
    }

    @PostMapping("/subscribe")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Subscribe device to web push notifications")
    public ResponseEntity<MessageResponse> subscribe(
            @Valid @RequestBody PushSubscriptionDto request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));

        webPushService.subscribe(user, request);
        return ResponseEntity.ok(MessageResponse.of("Push notification subscription registered successfully"));
    }

    @PostMapping("/unsubscribe")
    @Operation(summary = "Unsubscribe device from web push notifications")
    public ResponseEntity<MessageResponse> unsubscribe(@RequestBody Map<String, String> request) {
        String endpoint = request != null ? request.get("endpoint") : null;
        webPushService.unsubscribe(endpoint);
        return ResponseEntity.ok(MessageResponse.of("Push subscription removed"));
    }

    @PostMapping("/test")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Send a test push notification to current user")
    public ResponseEntity<MessageResponse> sendTestPush(@AuthenticationPrincipal AppUserPrincipal principal) {
        webPushService.sendPushToUser(
                principal.getId(),
                "🔔 NQD LMS - Kiểm tra thông báo",
                "Tuyệt vời! Điện thoại của bạn đã kết nối và nhận thông báo từ NQD LMS thành công.",
                "/"
        );
        return ResponseEntity.ok(MessageResponse.of("Test push notification dispatched"));
    }
}
