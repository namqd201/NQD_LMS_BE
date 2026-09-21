package com.nqd.nqd_lms_be.membership.service;

import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.BillingCycle;
import com.nqd.nqd_lms_be.entity.enums.EntitlementStatus;
import com.nqd.nqd_lms_be.entity.enums.EntitlementType;
import com.nqd.nqd_lms_be.entity.enums.SubscriptionStatus;
import com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse;
import com.nqd.nqd_lms_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final MembershipPlanService membershipPlanService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final EntitlementRepository entitlementRepository;

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrentSubscription(UUID userId) {
        Subscription sub = subscriptionRepository.findActiveSubscriptionByUser(userId, LocalDateTime.now()).orElse(null);
        if (sub == null) {
            // Build fallback response with default free plan
            User user = userRepository.findById(userId).orElse(null);
            MembershipPlan freePlan = membershipPlanService.getPlanEntityByCode("FREE_STUDENT");
            return SubscriptionResponse.builder()
                    .userId(userId)
                    .userEmail(user != null ? user.getEmail() : null)
                    .userFullName(user != null ? user.getFullName() : null)
                    .planId(freePlan != null ? freePlan.getId() : null)
                    .planCode(freePlan != null ? freePlan.getPlanCode() : null)
                    .planName(freePlan != null ? freePlan.getName() : null)
                    .plan(com.nqd.nqd_lms_be.membership.dto.MembershipPlanResponse.fromEntity(freePlan))
                    .status(SubscriptionStatus.ACTIVE)
                    .startDate(user != null ? user.getCreatedAt() : LocalDateTime.now())
                    .endDate(null)
                    .autoRenew(false)
                    .isCurrentlyActive(true)
                    .build();
        }
        return SubscriptionResponse.fromEntity(sub);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptionHistory(UUID userId) {
        return subscriptionRepository.findByUserIdAndIsDeletedFalseOrderByStartDateDesc(userId).stream()
                .map(SubscriptionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubscriptionResponse cancelAutoRenew(UUID userId, UUID subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói thuê bao với ID: " + subscriptionId));

        if (userId != null && !sub.getUser().getId().equals(userId)) {
            throw new ForbiddenOperationException("Bạn không có quyền quản lý gói thuê bao của người dùng khác.");
        }

        sub.setAutoRenew(false);
        sub.setCancelledAt(LocalDateTime.now());
        Subscription saved = subscriptionRepository.save(sub);

        log.info("Subscription #{} cancelled / auto-renew disabled by user {}", sub.getId(), sub.getUser().getEmail());
        return SubscriptionResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public SubscriptionResponse activateOrUpgradeSubscription(UUID userId, UUID planId, UUID sourceOrderId, Boolean autoRenew) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        MembershipPlan plan = membershipPlanService.getPlanEntityById(planId);
        Order order = sourceOrderId != null ? orderRepository.findById(sourceOrderId).orElse(null) : null;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now;
        LocalDateTime endDate = null;

        // Check if user currently has an active subscription for potential renewal extension
        List<Subscription> existingActiveSubs = subscriptionRepository.findActiveSubscriptionsByUserWithLock(userId);
        if (!existingActiveSubs.isEmpty()) {
            Subscription currentSub = existingActiveSubs.get(0);
            if (currentSub.getMembershipPlan().getId().equals(plan.getId()) && currentSub.getEndDate() != null && currentSub.getEndDate().isAfter(now)) {
                // Extension on same plan
                startDate = currentSub.getEndDate();
            } else {
                // Switching / upgrading plans -> mark old subscriptions as cancelled/superceded
                for (Subscription oldSub : existingActiveSubs) {
                    oldSub.setStatus(SubscriptionStatus.CANCELLED);
                    oldSub.setCancelledAt(now);
                    subscriptionRepository.save(oldSub);
                }
            }
        }

        // Calculate end date based on billing cycle
        if (plan.getBillingCycle() == BillingCycle.MONTHLY) {
            endDate = startDate.plusMonths(1);
        } else if (plan.getBillingCycle() == BillingCycle.QUARTERLY) {
            endDate = startDate.plusMonths(3);
        } else if (plan.getBillingCycle() == BillingCycle.YEARLY) {
            endDate = startDate.plusYears(1);
        }

        Subscription newSub = Subscription.builder()
                .user(user)
                .membershipPlan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(startDate.isBefore(now) ? now : startDate)
                .endDate(endDate)
                .autoRenew(autoRenew != null ? autoRenew : false)
                .sourceOrder(order)
                .build();

        Subscription saved = subscriptionRepository.save(newSub);

        // Provision Entitlement record
        Entitlement entitlement = Entitlement.builder()
                .user(user)
                .entitlementType(EntitlementType.MEMBERSHIP_BENEFIT)
                .targetEntityId(plan.getId())
                .status(EntitlementStatus.ACTIVE)
                .validFrom(newSub.getStartDate())
                .validUntil(newSub.getEndDate())
                .sourceOrder(order)
                .sourceSubscription(saved)
                .build();
        entitlementRepository.save(entitlement);

        log.info("Activated Subscription #{} for user {} on plan {} until {}",
                saved.getId(), user.getEmail(), plan.getName(), endDate);

        return SubscriptionResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubscriptionResponse> getAdminSubscriptions(SubscriptionStatus status, String keyword, Pageable pageable) {
        Specification<Subscription> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (keyword != null && !keyword.isBlank()) {
                String likeKw = "%" + keyword.trim().toLowerCase() + "%";
                var userEmailLike = cb.like(cb.lower(root.get("user").get("email")), likeKw);
                var userFullNameLike = cb.like(cb.lower(root.get("user").get("fullName")), likeKw);
                var planNameLike = cb.like(cb.lower(root.get("membershipPlan").get("name")), likeKw);
                var planCodeLike = cb.like(cb.lower(root.get("membershipPlan").get("planCode")), likeKw);
                predicates.add(cb.or(userEmailLike, userFullNameLike, planNameLike, planCodeLike));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Subscription> page = subscriptionRepository.findAll(spec, pageable);
        return PageResponse.fromPage(page, SubscriptionResponse::fromEntity);
    }

    @Override
    @Transactional
    public void expirePastDueSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> expiredSubs = subscriptionRepository.findExpiredActiveSubscriptions(now);

        if (expiredSubs.isEmpty()) {
            return;
        }

        log.info("Processing {} expired subscriptions past due as of {}", expiredSubs.size(), now);
        for (Subscription sub : expiredSubs) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);

            // Update associated entitlements
            List<Entitlement> entitlements = entitlementRepository.findByUserIdAndStatusAndIsDeletedFalse(
                    sub.getUser().getId(), EntitlementStatus.ACTIVE
            );
            for (Entitlement ent : entitlements) {
                if (ent.getSourceSubscription() != null && ent.getSourceSubscription().getId().equals(sub.getId())) {
                    ent.setStatus(EntitlementStatus.EXPIRED);
                    entitlementRepository.save(ent);
                }
            }
            log.info("Marked subscription #{} for user {} as EXPIRED", sub.getId(), sub.getUser().getEmail());
        }
    }
}
