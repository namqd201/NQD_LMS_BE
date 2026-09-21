package com.nqd.nqd_lms_be.controller.admin;

import com.nqd.nqd_lms_be.dto.admin.AdminUserResponse;
import com.nqd.nqd_lms_be.dto.admin.RoleDetailResponse;
import com.nqd.nqd_lms_be.dto.user.UserProfileResponse;
import com.nqd.nqd_lms_be.service.admin.AdminRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Role Management", description = "Endpoints for administrators to manage and inspect system roles and member assignments")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @GetMapping
    @Operation(summary = "Get all system roles with assigned user counts")
    public ResponseEntity<List<RoleDetailResponse>> getAllRoles() {
        return ResponseEntity.ok(adminRoleService.getAllRoles());
    }

    @GetMapping("/{roleName}/users")
    @Operation(summary = "Get all users assigned to a specific role")
    public ResponseEntity<List<UserProfileResponse>> getUsersByRoleName(@PathVariable String roleName) {
        return ResponseEntity.ok(adminRoleService.getUsersByRoleName(roleName));
    }

    @PostMapping("/users/{userId}/assign")
    @Operation(summary = "Assign a role to a user")
    public ResponseEntity<AdminUserResponse> assignRole(
            @PathVariable UUID userId,
            @RequestParam String roleName
    ) {
        return ResponseEntity.ok(adminRoleService.assignRole(userId, roleName));
    }

    @DeleteMapping("/users/{userId}/remove")
    @Operation(summary = "Remove a role from a user")
    public ResponseEntity<AdminUserResponse> removeRole(
            @PathVariable UUID userId,
            @RequestParam String roleName
    ) {
        return ResponseEntity.ok(adminRoleService.removeRole(userId, roleName));
    }
}
