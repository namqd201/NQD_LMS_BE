package com.nqd.nqd_lms_be.service.admin;

import com.nqd.nqd_lms_be.dto.admin.AdminUserResponse;
import com.nqd.nqd_lms_be.dto.admin.RoleDetailResponse;
import com.nqd.nqd_lms_be.dto.user.UserProfileResponse;

import java.util.List;
import java.util.UUID;

public interface AdminRoleService {
    List<RoleDetailResponse> getAllRoles();
    List<UserProfileResponse> getUsersByRoleName(String roleName);
    AdminUserResponse assignRole(UUID userId, String roleName);
    AdminUserResponse removeRole(UUID userId, String roleName);
}
