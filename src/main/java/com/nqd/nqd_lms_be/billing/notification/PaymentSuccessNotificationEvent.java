package com.nqd.nqd_lms_be.billing.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessNotificationEvent implements Serializable {

    private UUID userId;
    private String userEmail;
    private String userFullName;
    private String orderCode;
    private String productTitle;
    private BigDecimal amount;
    private String currency;
    private String paymentStatus;
    private LocalDateTime paidAt;
    private String accessUrl;
    private String instructions;
}
