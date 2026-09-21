package com.nqd.nqd_lms_be.controller.membership;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.entity.enums.PlanUserType;
import com.nqd.nqd_lms_be.membership.dto.MembershipPlanResponse;
import com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse;
import com.nqd.nqd_lms_be.membership.dto.UsageStatusResponse;
import com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService;
import com.nqd.nqd_lms_be.membership.service.MembershipPlanService;
import com.nqd.nqd_lms_be.membership.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/membership")
@RequiredArgsConstructor
@Tag(name = "Membership & Subscription", description = "Endpoints for viewing plans, managing subscriptions and checking usage quotas")
public class MembershipController {

    private final MembershipPlanService membershipPlanService;
    private final MembershipEntitlementService membershipEntitlementService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    @Operation(summary = "List all active membership plans (optionally filtered by userType: STUDENT/TEACHER)")
    public ResponseEntity<List<MembershipPlanResponse>> getPlans(
            @RequestParam(required = false) PlanUserType userType
    ) {
        return ResponseEntity.ok(membershipPlanService.getPublicPlans(userType));
    }

    @GetMapping("/plans/{id}")
    @Operation(summary = "Get membership plan details by ID")
    public ResponseEntity<MembershipPlanResponse> getPlanById(@PathVariable UUID id) {
        return ResponseEntity.ok(membershipPlanService.getPlanById(id));
    }

    @GetMapping("/subscription/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current active subscription, active plan, and effective benefits for logged-in user")
    public ResponseEntity<SubscriptionResponse> getCurrentSubscription(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription(principal.getId()));
    }

    @GetMapping("/subscription/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get full subscription history for logged-in user")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionHistory(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionHistory(principal.getId()));
    }

    @PostMapping("/subscription/{subscriptionId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel auto-renewal for a subscription (benefits remain valid until end date)")
    public ResponseEntity<SubscriptionResponse> cancelAutoRenew(
            @PathVariable UUID subscriptionId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(subscriptionService.cancelAutoRenew(principal.getId(), subscriptionId));
    }

    @GetMapping({"/usage", "/usage-status"})
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current usage quota, limits and remaining counts (AI questions, exams, classes, questions)")
    public ResponseEntity<UsageStatusResponse> getUsageStatus(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(membershipEntitlementService.getUsageStatus(principal.getId()));
    }
}
