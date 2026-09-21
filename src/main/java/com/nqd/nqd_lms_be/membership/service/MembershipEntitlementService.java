package com.nqd.nqd_lms_be.membership.service;

import com.nqd.nqd_lms_be.entity.MembershipPlan;
import com.nqd.nqd_lms_be.entity.Subscription;
import com.nqd.nqd_lms_be.entity.enums.FeatureKey;
import com.nqd.nqd_lms_be.membership.dto.UsageLimitCheckResult;
import com.nqd.nqd_lms_be.membership.dto.UsageStatusResponse;

import java.util.UUID;

public interface MembershipEntitlementService {

    boolean hasFeature(UUID userId, FeatureKey feature);

    UsageLimitCheckResult checkLimit(UUID userId, FeatureKey feature);

    void enforceAndConsumeUsage(UUID userId, FeatureKey feature, int amount);

    void enforceFeatureAccess(UUID userId, FeatureKey feature);

    UsageStatusResponse getUsageStatus(UUID userId);

    Subscription getEffectiveSubscription(UUID userId);

    MembershipPlan getEffectivePlan(UUID userId);
}
