package com.nqd.nqd_lms_be.controller.admin;

import com.nqd.nqd_lms_be.dto.admin.AdminGrantVipRequest;
import com.nqd.nqd_lms_be.dto.admin.AdminUserResponse;
import com.nqd.nqd_lms_be.dto.admin.UpdateUserRoleRequest;
import com.nqd.nqd_lms_be.dto.admin.UpdateUserStatusRequest;
import com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse;
import com.nqd.nqd_lms_be.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "Endpoints for administrator user management")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Get or search all users in the system")
    public ResponseEntity<List<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status
    ) {
        if ((query != null && !query.isBlank()) || (status != null && !status.isBlank())) {
            return ResponseEntity.ok(adminUserService.searchUsers(query, status));
        }
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Update roles assigned to user")
    public ResponseEntity<AdminUserResponse> updateUserRoles(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return ResponseEntity.ok(adminUserService.updateUserRoles(id, request));
    }

    @PostMapping("/{id}/roles")
    @Operation(summary = "Assign a role to user")
    public ResponseEntity<AdminUserResponse> assignRole(
            @PathVariable UUID id,
            @Valid @RequestBody com.nqd.nqd_lms_be.dto.admin.AssignRoleRequest request
    ) {
        return ResponseEntity.ok(adminUserService.assignRole(id, request.getRoleName()));
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    @Operation(summary = "Remove a role from user")
    public ResponseEntity<AdminUserResponse> removeRole(
            @PathVariable UUID id,
            @PathVariable String roleName
    ) {
        return ResponseEntity.ok(adminUserService.removeRole(id, roleName));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update user status (ACTIVE, INACTIVE, BANNED)")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(adminUserService.updateUserStatus(id, request));
    }

    @GetMapping("/{id}/subscription")
    @Operation(summary = "Get user current subscription and membership details")
    public ResponseEntity<SubscriptionResponse> getUserSubscription(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(adminUserService.getUserSubscription(id));
    }

    @PostMapping("/{id}/vip")
    @Operation(summary = "Grant or upgrade user to VIP/PRO free by Admin")
    public ResponseEntity<SubscriptionResponse> grantUserVip(
            @PathVariable UUID id,
            @RequestBody AdminGrantVipRequest request
    ) {
        return ResponseEntity.ok(adminUserService.grantUserVip(id, request));
    }

    @DeleteMapping("/{id}/subscription")
    @Operation(summary = "Revoke user active VIP/PRO subscription")
    public ResponseEntity<SubscriptionResponse> revokeUserVip(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(adminUserService.revokeUserVip(id, reason));
    }
}
