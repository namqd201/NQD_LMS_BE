package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "subscriptions",
    indexes = {
        @Index(name = "idx_subscriptions_user_status", columnList = "user_id, status"),
        @Index(name = "idx_subscriptions_end_date", columnList = "end_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class Subscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_plan_id", nullable = false)
    private MembershipPlan membershipPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private Boolean autoRenew = false;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_order_id")
    private Order sourceOrder;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == SubscriptionStatus.ACTIVE 
                && !startDate.isAfter(now) 
                && (endDate == null || endDate.isAfter(now));
    }
}
