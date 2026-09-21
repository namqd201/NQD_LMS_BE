package com.nqd.nqd_lms_be.membership.scheduler;

import com.nqd.nqd_lms_be.membership.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionExpirationScheduler {

    private final SubscriptionService subscriptionService;

    /**
     * Runs periodically every 5 minutes to sweep past-due active subscriptions and transition them to EXPIRED.
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void sweepExpiredSubscriptions() {
        try {
            subscriptionService.expirePastDueSubscriptions();
        } catch (Exception e) {
            log.error("Error during scheduled subscription expiration sweep: ", e);
        }
    }
}
