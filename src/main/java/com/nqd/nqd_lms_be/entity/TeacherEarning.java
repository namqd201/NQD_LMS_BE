package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.EarningStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "teacher_earnings",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_teacher_earning_order_item", columnNames = {"order_item_id"})
    },
    indexes = {
        @Index(name = "idx_teacher_earnings_teacher", columnList = "teacher_id"),
        @Index(name = "idx_teacher_earnings_status", columnList = "status"),
        @Index(name = "idx_teacher_earnings_available_at", columnList = "available_at"),
        @Index(name = "idx_teacher_earnings_order", columnList = "order_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class TeacherEarning extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "platform_fee_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal platformFeeRate;

    @Column(name = "platform_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "teacher_share_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal teacherShareRate;

    @Column(name = "teacher_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal teacherAmount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EarningStatus status = EarningStatus.PENDING;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "reversal_reason", columnDefinition = "TEXT")
    private String reversalReason;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    public boolean isAvailable(LocalDateTime now) {
        if (status == EarningStatus.AVAILABLE) {
            return true;
        }
        if (status == EarningStatus.PENDING && availableAt != null && !availableAt.isAfter(now)) {
            return true;
        }
        return false;
    }
}
