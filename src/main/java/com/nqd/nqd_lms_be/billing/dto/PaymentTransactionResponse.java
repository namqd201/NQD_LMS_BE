package com.nqd.nqd_lms_be.billing.dto;

import com.nqd.nqd_lms_be.entity.PaymentTransaction;
import com.nqd.nqd_lms_be.entity.enums.PaymentMethod;
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
public class PaymentTransactionResponse {

    private UUID id;
    private String transactionCode;
    private UUID orderId;
    private String orderCode;
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private String provider;
    private String providerTransactionId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String gatewayReference;
    private String failureReason;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    public static PaymentTransactionResponse fromEntity(PaymentTransaction txn) {
        return PaymentTransactionResponse.builder()
                .id(txn.getId())
                .transactionCode(txn.getTransactionCode())
                .orderId(txn.getOrder() != null ? txn.getOrder().getId() : null)
                .orderCode(txn.getOrder() != null ? txn.getOrder().getOrderCode() : null)
                .userId(txn.getOrder() != null && txn.getOrder().getUser() != null ? txn.getOrder().getUser().getId() : null)
                .userEmail(txn.getOrder() != null && txn.getOrder().getUser() != null ? txn.getOrder().getUser().getEmail() : null)
                .userFullName(txn.getOrder() != null && txn.getOrder().getUser() != null ? txn.getOrder().getUser().getFullName() : null)
                .provider(txn.getProvider())
                .providerTransactionId(txn.getProviderTransactionId())
                .amount(txn.getAmount())
                .currency(txn.getCurrency())
                .status(txn.getStatus())
                .paymentMethod(txn.getPaymentMethod())
                .gatewayReference(txn.getGatewayReference())
                .failureReason(txn.getFailureReason())
                .paidAt(txn.getPaidAt())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
