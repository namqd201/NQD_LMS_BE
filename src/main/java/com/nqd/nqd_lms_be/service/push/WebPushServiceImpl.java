package com.nqd.nqd_lms_be.service.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.dto.push.PushSubscriptionDto;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.UserPushSubscription;
import com.nqd.nqd_lms_be.repository.UserPushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Security;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushServiceImpl implements WebPushService {

    private final UserPushSubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;

    @Value("${webpush.vapid.public-key:BBhzeAGIXNZjfC7AfOkN-gVsclnjHxI-HO5cUqB-dGmRXllMN9eROU9urks7BruiqjmPUNSRzTRTpSZSZoqQZV8}")
    private String vapidPublicKey;

    @Value("${webpush.vapid.private-key:E-e_Yy5_MBDKZUs-duWzfaH5G_JndriCe6WLzOkUet4}")
    private String vapidPrivateKey;

    @Value("${webpush.vapid.subject:mailto:admin@nqdlms.online}")
    private String vapidSubject;

    private PushService pushService;

    @PostConstruct
    public void init() {
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            if (vapidPublicKey != null && !vapidPublicKey.isBlank()
                    && vapidPrivateKey != null && !vapidPrivateKey.isBlank()) {
                pushService = new PushService();
                pushService.setPublicKey(vapidPublicKey.trim());
                pushService.setPrivateKey(vapidPrivateKey.trim());
                pushService.setSubject(vapidSubject.trim());
                log.info("WebPushService successfully initialized with VAPID subject: {}", vapidSubject);
            } else {
                log.warn("WebPushService initialized without complete VAPID keys; push notifications will be skipped.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize WebPushService with VAPID keys: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    @Override
    @Transactional
    public void subscribe(User user, PushSubscriptionDto request) {
        if (user == null || request == null || request.getEndpoint() == null || request.getEndpoint().isBlank()) {
            return;
        }

        Optional<UserPushSubscription> existing = subscriptionRepository.findByEndpoint(request.getEndpoint().trim());
        if (existing.isPresent()) {
            UserPushSubscription sub = existing.get();
            sub.setUser(user);
            sub.setP256dh(request.getP256dh().trim());
            sub.setAuth(request.getAuth().trim());
            sub.setUserAgent(request.getUserAgent());
            subscriptionRepository.save(sub);
            log.info("Updated existing push subscription for user: {}", user.getEmail());
        } else {
            UserPushSubscription sub = UserPushSubscription.builder()
                    .user(user)
                    .endpoint(request.getEndpoint().trim())
                    .p256dh(request.getP256dh().trim())
                    .auth(request.getAuth().trim())
                    .userAgent(request.getUserAgent())
                    .build();
            subscriptionRepository.save(sub);
            log.info("Saved new push subscription for user: {}", user.getEmail());
        }
    }

    @Override
    @Transactional
    public void unsubscribe(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return;
        }
        try {
            subscriptionRepository.deleteByEndpoint(endpoint.trim());
            log.info("Removed push subscription for endpoint: {}", endpoint);
        } catch (Exception e) {
            log.warn("Failed to delete push subscription: {}", e.getMessage());
        }
    }

    @Override
    @Async
    public void sendPushToUser(UUID userId, String title, String body, String linkUrl) {
        if (pushService == null || userId == null) {
            return;
        }

        List<UserPushSubscription> subscriptions = subscriptionRepository.findByUserId(userId);
        if (subscriptions.isEmpty()) {
            return;
        }

        for (UserPushSubscription sub : subscriptions) {
            dispatchPush(sub, title, body, linkUrl);
        }
    }

    @Override
    @Async
    public void sendPushToUsers(List<UUID> userIds, String title, String body, String linkUrl) {
        if (pushService == null || userIds == null || userIds.isEmpty()) {
            return;
        }

        List<UserPushSubscription> subscriptions = subscriptionRepository.findByUserIdIn(userIds);
        for (UserPushSubscription sub : subscriptions) {
            dispatchPush(sub, title, body, linkUrl);
        }
    }

    private void dispatchPush(UserPushSubscription sub, String title, String body, String linkUrl) {
        try {
            Subscription subscription = new Subscription(
                    sub.getEndpoint(),
                    new Subscription.Keys(sub.getP256dh(), sub.getAuth())
            );

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("title", title);
            payloadMap.put("body", body);
            payloadMap.put("linkUrl", linkUrl != null ? linkUrl : "/");
            payloadMap.put("icon", "/logo.png");
            payloadMap.put("badge", "/logo.png");

            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            Notification notification = new Notification(subscription, payloadJson);

            HttpResponse response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 201 || statusCode == 200) {
                log.info("Push notification sent successfully to endpoint (status={})", statusCode);
            } else if (statusCode == 404 || statusCode == 410) {
                // Subscription has expired or user unsubscribed on device
                log.info("Push subscription expired on device (status={}), cleaning up from DB", statusCode);
                try {
                    subscriptionRepository.delete(sub);
                } catch (Exception ignored) {}
            } else {
                log.warn("Push notification returned status={}", statusCode);
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch push notification to endpoint {}: {}", sub.getEndpoint(), e.getMessage());
        }
    }
}
