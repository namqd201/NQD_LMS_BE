package com.nqd.nqd_lms_be.billing.notification;

import com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingNotificationService {

    private final KafkaNotificationProducer kafkaNotificationProducer;

    public void sendPaymentSuccessNotification(PaymentSuccessNotificationEvent event) {
        try {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            String formattedAmount = currencyFormat.format(event.getAmount());
            String formattedTime = event.getPaidAt() != null
                    ? event.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                    : "";

            String title = "Thanh toán thành công đơn hàng #" + event.getOrderCode();
            String body = String.format(
                    "Chúc mừng bạn đã thanh toán thành công số tiền %s cho '%s'. Thời gian: %s. %s",
                    formattedAmount,
                    event.getProductTitle() != null ? event.getProductTitle() : "sản phẩm đã mua",
                    formattedTime,
                    event.getInstructions() != null ? event.getInstructions() : "Quyền truy cập học tập đã được kích hoạt."
            );

            log.info("Sending payment success notification to user {}: order={}, amount={}",
                    event.getUserId(), event.getOrderCode(), formattedAmount);

            String linkUrl = event.getAccessUrl() != null ? event.getAccessUrl() : "/student/courses";
            kafkaNotificationProducer.sendNotification(event.getUserId(), "PAYMENT_SUCCESS", title, body, linkUrl);
        } catch (Exception e) {
            log.error("Failed to dispatch payment success notification for order {}: ", event.getOrderCode(), e);
        }
    }
}
