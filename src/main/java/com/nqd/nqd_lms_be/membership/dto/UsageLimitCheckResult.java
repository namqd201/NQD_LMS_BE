package com.nqd.nqd_lms_be.membership.dto;

import com.nqd.nqd_lms_be.entity.enums.FeatureKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageLimitCheckResult {

    private boolean allowed;
    private FeatureKey featureKey;
    private Integer limit; // null or -1 = UNLIMITED
    private Integer currentUsage;
    private Integer remaining;
    private String reason;

    public static UsageLimitCheckResult allowedUnlimited(FeatureKey featureKey, int currentUsage) {
        return UsageLimitCheckResult.builder()
                .allowed(true)
                .featureKey(featureKey)
                .limit(-1)
                .currentUsage(currentUsage)
                .remaining(null)
                .reason("Không giới hạn cho gói dịch vụ hiện tại")
                .build();
    }

    public static UsageLimitCheckResult allowedWithinLimit(FeatureKey featureKey, int limit, int currentUsage) {
        return UsageLimitCheckResult.builder()
                .allowed(true)
                .featureKey(featureKey)
                .limit(limit)
                .currentUsage(currentUsage)
                .remaining(Math.max(0, limit - currentUsage))
                .reason("Còn lượt sử dụng")
                .build();
    }

    public static UsageLimitCheckResult exceeded(FeatureKey featureKey, int limit, int currentUsage, String reason) {
        return UsageLimitCheckResult.builder()
                .allowed(false)
                .featureKey(featureKey)
                .limit(limit)
                .currentUsage(currentUsage)
                .remaining(0)
                .reason(reason)
                .build();
    }
}
