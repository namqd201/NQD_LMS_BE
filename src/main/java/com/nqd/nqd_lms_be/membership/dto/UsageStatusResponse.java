package com.nqd.nqd_lms_be.membership.dto;

import com.nqd.nqd_lms_be.entity.enums.PlanUserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageStatusResponse {

    private UUID userId;
    private String userEmail;
    private PlanUserType userType;
    private String currentPlanCode;
    private String currentPlanName;
    private boolean isPremium;
    private Set<String> activeFeatures;
    private List<FeatureUsageDto> usageMetrics;
}
