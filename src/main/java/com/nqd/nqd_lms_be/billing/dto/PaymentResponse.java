package com.nqd.nqd_lms_be.billing.dto;

import com.nqd.nqd_lms_be.entity.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private UUID paymentId;
    private String transactionCode;
    private UUID orderId;
    private String orderCode;
    private BigDecimal amount;
    private String currency;
    private String provider;
    private PaymentStatus status;
    private String qrCode;
    private String checkoutUrl;
    private String signature;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
