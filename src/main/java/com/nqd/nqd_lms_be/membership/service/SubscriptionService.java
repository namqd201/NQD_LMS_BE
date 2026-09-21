package com.nqd.nqd_lms_be.membership.service;

import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.entity.enums.SubscriptionStatus;
import com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {

    SubscriptionResponse getCurrentSubscription(UUID userId);

    List<SubscriptionResponse> getSubscriptionHistory(UUID userId);

    SubscriptionResponse cancelAutoRenew(UUID userId, UUID subscriptionId);

    SubscriptionResponse activateOrUpgradeSubscription(UUID userId, UUID planId, UUID sourceOrderId, Boolean autoRenew);

    PageResponse<SubscriptionResponse> getAdminSubscriptions(
            SubscriptionStatus status,
            String keyword,
            Pageable pageable
    );

    void expirePastDueSubscriptions();
}
