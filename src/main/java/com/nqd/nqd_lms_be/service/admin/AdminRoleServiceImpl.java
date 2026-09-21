package com.nqd.nqd_lms_be.service.admin;

import com.nqd.nqd_lms_be.dto.admin.AdminUserResponse;
import com.nqd.nqd_lms_be.dto.admin.RoleDetailResponse;
import com.nqd.nqd_lms_be.dto.user.UserProfileResponse;
import com.nqd.nqd_lms_be.entity.Role;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.repository.RoleRepository;
import com.nqd.nqd_lms_be.repository.UserRepository;
import com.nqd.nqd_lms_be.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleServiceImpl implements AdminRoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final AdminUserService adminUserService;

    @Override
    @Transactional(readOnly = true)
    public List<RoleDetailResponse> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return roles.stream().map(r -> {
            long userCount = userRoleRepository.countByRoleId(r.getId());
            return RoleDetailResponse.builder()
                    .id(r.getId())
                    .name(r.getName())
                    .description(r.getDescription())
                    .userCount(userCount)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getUsersByRoleName(String roleName) {
        String cleanRole = roleName.replace("ROLE_", "").toUpperCase();
        List<User> users = userRoleRepository.findUsersByRoleName(cleanRole);
        return users.stream().map(this::mapToUserProfileResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdminUserResponse assignRole(UUID userId, String roleName) {
        return adminUserService.assignRole(userId, roleName);
    }

    @Override
    @Transactional
    public AdminUserResponse removeRole(UUID userId, String roleName) {
        return adminUserService.removeRole(userId, roleName);
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .roles(new HashSet<>(roles))
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
