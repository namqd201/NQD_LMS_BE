package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.FeatureKey;
import com.nqd.nqd_lms_be.entity.enums.UsagePeriodType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_usage_records",
    indexes = {
        @Index(name = "idx_usage_user_feature_period", columnList = "user_id, feature_key, period_key", unique = true),
        @Index(name = "idx_usage_user_id", columnList = "user_id"),
        @Index(name = "idx_usage_period_key", columnList = "period_key")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class UserUsageRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_key", nullable = false, length = 64)
    private FeatureKey featureKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 32)
    @Builder.Default
    private UsagePeriodType periodType = UsagePeriodType.DAILY;

    /**
     * Period identifier:
     * - Daily: YYYY-MM-DD (e.g. 2026-09-06)
     * - Weekly: YYYY-Www (e.g. 2026-W36)
     * - Monthly: YYYY-MM (e.g. 2026-09)
     * - Total: TOTAL
     */
    @Column(name = "period_key", nullable = false, length = 32)
    private String periodKey;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "last_consumed_at")
    private LocalDateTime lastConsumedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    public void incrementUsage(int amount) {
        if (this.usageCount == null) {
            this.usageCount = 0;
        }
        this.usageCount += amount;
        this.lastConsumedAt = LocalDateTime.now();
    }
}
