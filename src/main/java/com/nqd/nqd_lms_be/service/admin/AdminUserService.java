package com.nqd.nqd_lms_be.service.admin;

import com.nqd.nqd_lms_be.dto.admin.AdminGrantVipRequest;
import com.nqd.nqd_lms_be.dto.admin.AdminUserResponse;
import com.nqd.nqd_lms_be.dto.admin.RoleResponse;
import com.nqd.nqd_lms_be.dto.admin.UpdateUserRoleRequest;
import com.nqd.nqd_lms_be.dto.admin.UpdateUserStatusRequest;
import com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse;

import java.util.List;
import java.util.UUID;

public interface AdminUserService {
    List<AdminUserResponse> getAllUsers();
    List<AdminUserResponse> searchUsers(String query, String status);
    AdminUserResponse getUserById(UUID id);
    AdminUserResponse updateUserRoles(UUID id, UpdateUserRoleRequest request);
    AdminUserResponse assignRole(UUID id, String roleName);
    AdminUserResponse removeRole(UUID id, String roleName);
    AdminUserResponse updateUserStatus(UUID id, UpdateUserStatusRequest request);
    List<RoleResponse> getAllRoles();
    SubscriptionResponse getUserSubscription(UUID userId);
    SubscriptionResponse grantUserVip(UUID userId, AdminGrantVipRequest request);
    SubscriptionResponse revokeUserVip(UUID userId, String reason);
}
