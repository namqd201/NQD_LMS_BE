package com.nqd.nqd_lms_be.membership.dto;

import com.nqd.nqd_lms_be.entity.enums.FeatureKey;
import com.nqd.nqd_lms_be.entity.enums.UsagePeriodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureUsageDto {
    private FeatureKey featureKey;
    private String featureName;
    private UsagePeriodType periodType;
    private String periodKey;
    private Integer usageCount;
    private Integer limit; // null or -1 means UNLIMITED
    private Integer remaining; // null means UNLIMITED
    private boolean isExceeded;
    private LocalDateTime resetsAt;
}
