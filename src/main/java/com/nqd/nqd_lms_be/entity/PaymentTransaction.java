package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.PaymentMethod;
import com.nqd.nqd_lms_be.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "payment_transactions",
    indexes = {
        @Index(name = "idx_payment_txn_code", columnList = "transaction_code", unique = true),
        @Index(name = "idx_payment_txn_order", columnList = "order_id"),
        @Index(name = "idx_payment_txn_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_payment_txn_provider_tx", columnList = "provider, provider_transaction_id"),
        @Index(name = "idx_payment_txn_status", columnList = "status"),
        @Index(name = "idx_payment_txn_gateway_ref", columnList = "gateway_reference")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class PaymentTransaction extends BaseEntity {

    @Column(name = "transaction_code", nullable = false, unique = true, length = 64)
    private String transactionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "provider", length = 32)
    @Builder.Default
    private String provider = "PAYOS";

    @Column(name = "provider_transaction_id", length = 128)
    private String providerTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 32)
    private PaymentMethod paymentMethod;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "gateway_reference", length = 512)
    private String gatewayReference;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "raw_reference", columnDefinition = "TEXT")
    private String rawReference;

    @Column(name = "raw_callback_payload", columnDefinition = "TEXT")
    private String rawCallbackPayload;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
