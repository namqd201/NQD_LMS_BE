package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.WithdrawalStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "teacher_withdrawals",
    indexes = {
        @Index(name = "idx_teacher_withdrawals_code", columnList = "withdrawal_code"),
        @Index(name = "idx_teacher_withdrawals_teacher", columnList = "teacher_id"),
        @Index(name = "idx_teacher_withdrawals_status", columnList = "status"),
        @Index(name = "idx_teacher_withdrawals_idempotency", columnList = "idempotency_key")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class TeacherWithdrawal extends BaseEntity {

    @Column(name = "withdrawal_code", nullable = false, unique = true, length = 64)
    private String withdrawalCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "bank_name", nullable = false, length = 255)
    private String bankName;

    @Column(name = "bank_code", nullable = false, length = 50)
    private String bankCode;

    @Column(name = "account_number", nullable = false, length = 100)
    private String accountNumber;

    @Column(name = "account_holder_name", nullable = false, length = 255)
    private String accountHolderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "reference_code", length = 128)
    private String referenceCode;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by", length = 255)
    private String processedBy;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        int len = accountNumber.length();
        return "*".repeat(len - 4) + accountNumber.substring(len - 4);
    }
}
