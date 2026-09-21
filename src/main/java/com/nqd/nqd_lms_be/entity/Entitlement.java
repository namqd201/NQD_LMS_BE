package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.EntitlementStatus;
import com.nqd.nqd_lms_be.entity.enums.EntitlementType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "entitlements",
    indexes = {
        @Index(name = "idx_entitlements_user_type_target", columnList = "user_id, entitlement_type, target_entity_id"),
        @Index(name = "idx_entitlements_user_status", columnList = "user_id, status"),
        @Index(name = "idx_entitlements_valid_until", columnList = "valid_until")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class Entitlement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "entitlement_type", nullable = false, length = 32)
    private EntitlementType entitlementType;

    /**
     * Target resource ID (e.g. course_id, exam_id) that this entitlement grants access to.
     */
    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EntitlementStatus status = EntitlementStatus.ACTIVE;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_order_id")
    private Order sourceOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_subscription_id")
    private Subscription sourceSubscription;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    public boolean isCurrentlyValid() {
        LocalDateTime now = LocalDateTime.now();
        return status == EntitlementStatus.ACTIVE
                && !validFrom.isAfter(now)
                && (validUntil == null || validUntil.isAfter(now));
    }
}
