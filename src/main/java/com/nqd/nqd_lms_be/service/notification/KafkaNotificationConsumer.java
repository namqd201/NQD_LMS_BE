package com.nqd.nqd_lms_be.service.notification;

import com.nqd.nqd_lms_be.config.KafkaConfig;
import com.nqd.nqd_lms_be.entity.Notification;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.event.NotificationEvent;
import com.nqd.nqd_lms_be.repository.NotificationRepository;
import com.nqd.nqd_lms_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @KafkaListener(topics = KafkaConfig.NOTIFICATION_TOPIC, groupId = "nqd-lms-group")
    @Transactional
    public void consumeNotificationEvent(NotificationEvent event) {
        log.info("Received Kafka notification event for user {}: {}", event.getUserId(), event.getTitle());

        try {
            User user = userRepository.findById(event.getUserId()).orElse(null);
            if (user == null) {
                log.warn("Cannot process notification: User {} not found", event.getUserId());
                return;
            }

            Notification notification = Notification.builder()
                    .user(user)
                    .type(event.getType())
                    .title(event.getTitle())
                    .body(event.getBody())
                    .linkUrl(event.getLinkUrl())
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);
            log.info("Notification successfully persisted to DB for user {}", user.getEmail());
        } catch (Exception ex) {
            log.error("Failed to process consumed notification event: {}", ex.getMessage(), ex);
        }
    }
}
