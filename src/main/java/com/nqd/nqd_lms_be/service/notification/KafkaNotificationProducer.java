package com.nqd.nqd_lms_be.service.notification;

import com.nqd.nqd_lms_be.config.KafkaConfig;
import com.nqd.nqd_lms_be.entity.Notification;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.event.NotificationEvent;
import com.nqd.nqd_lms_be.repository.NotificationRepository;
import com.nqd.nqd_lms_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public void sendNotification(UUID userId, String type, String title, String body, String linkUrl) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .linkUrl(linkUrl)
                .build();

        try {
            log.info("Publishing notification event to Kafka topic {}: type={}, user={}",
                    KafkaConfig.NOTIFICATION_TOPIC, type, userId);
            kafkaTemplate.send(KafkaConfig.NOTIFICATION_TOPIC, userId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Kafka send async failure, falling back to direct DB persistence: {}", ex.getMessage());
                            saveDirectlyToDatabase(event);
                        } else {
                            log.info("Kafka notification event sent successfully to partition {}",
                                    result.getRecordMetadata().partition());
                        }
                    });
        } catch (Exception e) {
            log.warn("Failed to publish to Kafka (sync error), saving directly to DB: {}", e.getMessage());
            saveDirectlyToDatabase(event);
        }
    }

    public void saveDirectlyToDatabase(NotificationEvent event) {
        try {
            User user = userRepository.findById(event.getUserId()).orElse(null);
            if (user == null) {
                log.warn("Cannot save notification: User {} not found", event.getUserId());
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
            log.info("Notification saved directly to DB for user {}", user.getEmail());
        } catch (Exception ex) {
            log.error("Error directly saving notification to database: {}", ex.getMessage(), ex);
        }
    }
}
