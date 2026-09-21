package com.nqd.nqd_lms_be.controller.admin;

import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.entity.enums.SubscriptionStatus;
import com.nqd.nqd_lms_be.membership.dto.CreateMembershipPlanRequest;
import com.nqd.nqd_lms_be.membership.dto.MembershipPlanResponse;
import com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse;
import com.nqd.nqd_lms_be.membership.dto.UpdateMembershipPlanRequest;
import com.nqd.nqd_lms_be.membership.service.MembershipPlanService;
import com.nqd.nqd_lms_be.membership.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/membership")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Membership & Subscriptions", description = "Endpoints for platform administrators to manage membership plans and view user subscriptions")
public class AdminMembershipController {

    private final MembershipPlanService membershipPlanService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    @Operation(summary = "List all membership plans (including disabled and drafts)")
    public ResponseEntity<List<MembershipPlanResponse>> getAllPlans() {
        return ResponseEntity.ok(membershipPlanService.getAllPlansAdmin());
    }

    @GetMapping("/plans/{id}")
    @Operation(summary = "Get membership plan by ID")
    public ResponseEntity<MembershipPlanResponse> getPlanById(@PathVariable UUID id) {
        return ResponseEntity.ok(membershipPlanService.getPlanById(id));
    }

    @PostMapping("/plans")
    @Operation(summary = "Create a new membership plan")
    public ResponseEntity<MembershipPlanResponse> createPlan(@Valid @RequestBody CreateMembershipPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(membershipPlanService.createPlan(request));
    }

    @PutMapping("/plans/{id}")
    @Operation(summary = "Update an existing membership plan")
    public ResponseEntity<MembershipPlanResponse> updatePlan(
            @PathVariable UUID id,
            @RequestBody UpdateMembershipPlanRequest request
    ) {
        return ResponseEntity.ok(membershipPlanService.updatePlan(id, request));
    }

    @PatchMapping("/plans/{id}/status")
    @Operation(summary = "Toggle active/inactive status of a plan")
    public ResponseEntity<MembershipPlanResponse> togglePlanStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body
    ) {
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(membershipPlanService.togglePlanStatus(id, active));
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "List, search, and filter subscriptions across all users")
    public ResponseEntity<PageResponse<SubscriptionResponse>> getSubscriptions(
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(subscriptionService.getAdminSubscriptions(status, keyword, pageable));
    }
}
