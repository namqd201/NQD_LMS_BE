package com.nqd.nqd_lms_be.service.admin;

import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.admin.AdminGrantVipRequest;
import com.nqd.nqd_lms_be.dto.admin.AdminUserResponse;
import com.nqd.nqd_lms_be.dto.admin.RoleResponse;
import com.nqd.nqd_lms_be.dto.admin.UpdateUserRoleRequest;
import com.nqd.nqd_lms_be.dto.admin.UpdateUserStatusRequest;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.BillingCycle;
import com.nqd.nqd_lms_be.entity.enums.EntitlementStatus;
import com.nqd.nqd_lms_be.entity.enums.EntitlementType;
import com.nqd.nqd_lms_be.entity.enums.SubscriptionStatus;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse;
import com.nqd.nqd_lms_be.membership.service.MembershipPlanService;
import com.nqd.nqd_lms_be.membership.service.SubscriptionService;
import com.nqd.nqd_lms_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final MembershipPlanService membershipPlanService;
    private final EntitlementRepository entitlementRepository;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        Map<UUID, Subscription> activeMap = getActiveSubscriptionsMap();
        return users.stream()
                .map(u -> mapToAdminUserResponse(u, activeMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> searchUsers(String query, String status) {
        UserStatus userStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                userStatus = UserStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<User> users = userRepository.searchUsers(query, userStatus);
        Map<UUID, Subscription> activeMap = getActiveSubscriptionsMap();
        return users.stream()
                .map(u -> mapToAdminUserResponse(u, activeMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        Subscription sub = subscriptionRepository.findActiveSubscriptionByUser(user.getId(), LocalDateTime.now()).orElse(null);
        Map<UUID, Subscription> map = sub != null ? Map.of(user.getId(), sub) : Collections.emptyMap();
        return mapToAdminUserResponse(user, map);
    }

    @Override
    @Transactional
    public AdminUserResponse assignRole(UUID id, String roleName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        String cleanRoleName = roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
        Role role = roleRepository.findByName(cleanRoleName)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(cleanRoleName)
                        .description(cleanRoleName + " role")
                        .build()));

        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            UserRole userRole = UserRole.builder()
                    .userId(user.getId())
                    .roleId(role.getId())
                    .user(user)
                    .role(role)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRoleRepository.save(userRole);
            log.info("Admin assigned role {} to user {}", cleanRoleName, user.getEmail());
        }

        return getUserById(id);
    }

    @Override
    @Transactional
    public AdminUserResponse removeRole(UUID id, String roleName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        String cleanRoleName = roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
        roleRepository.findByName(cleanRoleName).ifPresent(role -> {
            userRoleRepository.deleteByUserIdAndRoleId(user.getId(), role.getId());
            log.info("Admin removed role {} from user {}", cleanRoleName, user.getEmail());
        });

        return getUserById(id);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserRoles(UUID id, UpdateUserRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // Delete existing roles
        userRoleRepository.deleteByUserId(user.getId());

        // Assign new roles
        for (String roleName : request.getRoles()) {
            String cleanRoleName = roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
            Role role = roleRepository.findByName(cleanRoleName)
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name(cleanRoleName)
                            .description(cleanRoleName + " role")
                            .build()));

            UserRole userRole = UserRole.builder()
                    .userId(user.getId())
                    .roleId(role.getId())
                    .user(user)
                    .role(role)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRoleRepository.save(userRole);
        }

        log.info("Admin updated roles for user: {} to {}", user.getEmail(), request.getRoles());
        return getUserById(id);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserStatus(UUID id, UpdateUserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        user.setStatus(request.getStatus());
        user = userRepository.save(user);

        log.info("Admin updated status for user: {} to {}", user.getEmail(), request.getStatus());
        return getUserById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> RoleResponse.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .description(role.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getUserSubscription(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return subscriptionService.getCurrentSubscription(user.getId());
    }

    @Override
    @Transactional
    public SubscriptionResponse grantUserVip(UUID userId, AdminGrantVipRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Resolve plan
        MembershipPlan plan = null;
        if (request != null && request.getPlanId() != null) {
            plan = membershipPlanRepository.findById(request.getPlanId()).orElse(null);
        }

        if (plan == null && request != null && request.getPlanCode() != null && !request.getPlanCode().isBlank()) {
            String codeOrName = request.getPlanCode().trim();
            // 1. Try exact planCode
            plan = membershipPlanRepository.findByPlanCodeAndIsDeletedFalse(codeOrName).orElse(null);

            // 2. Try case-insensitive or name match (in case frontend passed plan name or display text)
            if (plan == null) {
                plan = membershipPlanRepository.findAll().stream()
                        .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                        .filter(p -> p.getPlanCode().equalsIgnoreCase(codeOrName)
                                || (p.getName() != null && p.getName().equalsIgnoreCase(codeOrName))
                                || (p.getName() != null && codeOrName.toLowerCase().contains(p.getName().toLowerCase()))
                                || (p.getName() != null && p.getName().toLowerCase().contains(codeOrName.toLowerCase())))
                        .findFirst()
                        .orElse(null);
            }
        }

        if (plan == null) {
            // Default plan based on user roles
            List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
            boolean isTeacher = roles.stream().anyMatch(r -> r.contains("TEACHER"));
            String defaultCode = isTeacher ? "TEACHER_PRO_MONTHLY" : "VIP_STUDENT_MONTHLY";
            plan = membershipPlanRepository.findByPlanCodeAndIsDeletedFalse(defaultCode)
                    .orElseGet(() -> membershipPlanService.getPlanEntityByCode(defaultCode));
        }

        if (plan == null) {
            throw new ResourceNotFoundException("Không tìm thấy gói hội viên phù hợp để cấp quyền VIP.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now;
        LocalDateTime endDate;

        Integer duration = request != null ? request.getDurationMonths() : null;
        if (duration != null) {
            if (duration == -1 || duration >= 100) {
                endDate = startDate.plusYears(100); // Lifetime
            } else {
                endDate = startDate.plusMonths(Math.max(1, duration));
            }
        } else {
            if (plan.getBillingCycle() == BillingCycle.MONTHLY) {
                endDate = startDate.plusMonths(1);
            } else if (plan.getBillingCycle() == BillingCycle.QUARTERLY) {
                endDate = startDate.plusMonths(3);
            } else if (plan.getBillingCycle() == BillingCycle.YEARLY) {
                endDate = startDate.plusYears(1);
            } else {
                endDate = startDate.plusYears(100);
            }
        }

        // Cancel existing active subscriptions
        List<Subscription> existingSubs = subscriptionRepository.findActiveSubscriptionsByUserList(userId, now);
        for (Subscription oldSub : existingSubs) {
            oldSub.setStatus(SubscriptionStatus.CANCELLED);
            oldSub.setCancelledAt(now);
            subscriptionRepository.save(oldSub);

            List<Entitlement> oldEnts = entitlementRepository.findBySourceSubscriptionIdAndIsDeletedFalse(oldSub.getId());
            for (Entitlement e : oldEnts) {
                e.setStatus(EntitlementStatus.REVOKED);
                entitlementRepository.save(e);
            }
        }

        // Create new active Subscription
        Subscription newSub = Subscription.builder()
                .user(user)
                .membershipPlan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(startDate)
                .endDate(endDate)
                .autoRenew(false)
                .sourceOrder(null)
                .build();
        Subscription savedSub = subscriptionRepository.save(newSub);

        // Create active Entitlement
        Entitlement entitlement = Entitlement.builder()
                .user(user)
                .entitlementType(EntitlementType.MEMBERSHIP_BENEFIT)
                .targetEntityId(plan.getId())
                .status(EntitlementStatus.ACTIVE)
                .validFrom(startDate)
                .validUntil(endDate)
                .sourceOrder(null)
                .sourceSubscription(savedSub)
                .build();
        entitlementRepository.save(entitlement);

        log.info("Admin granted VIP/PRO plan '{}' ({}) to user {} until {}. Reason: {}",
                plan.getName(), plan.getPlanCode(), user.getEmail(), endDate, request != null ? request.getReason() : null);

        return SubscriptionResponse.fromEntity(savedSub);
    }

    @Override
    @Transactional
    public SubscriptionResponse revokeUserVip(UUID userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        LocalDateTime now = LocalDateTime.now();
        List<Subscription> existingSubs = subscriptionRepository.findActiveSubscriptionsByUserList(userId, now);
        for (Subscription oldSub : existingSubs) {
            oldSub.setStatus(SubscriptionStatus.CANCELLED);
            oldSub.setCancelledAt(now);
            subscriptionRepository.save(oldSub);

            List<Entitlement> oldEnts = entitlementRepository.findBySourceSubscriptionIdAndIsDeletedFalse(oldSub.getId());
            for (Entitlement e : oldEnts) {
                e.setStatus(EntitlementStatus.REVOKED);
                entitlementRepository.save(e);
            }
        }

        log.info("Admin revoked VIP/PRO subscription for user {}. Reason: {}", user.getEmail(), reason);
        return subscriptionService.getCurrentSubscription(userId);
    }

    private Map<UUID, Subscription> getActiveSubscriptionsMap() {
        try {
            return subscriptionRepository.findAllActiveSubscriptions(LocalDateTime.now()).stream()
                    .collect(Collectors.toMap(s -> s.getUser().getId(), s -> s, (existing, replacement) -> existing));
        } catch (Exception e) {
            log.warn("Failed to fetch active subscriptions map: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private AdminUserResponse mapToAdminUserResponse(User user, Map<UUID, Subscription> activeSubsMap) {
        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        Subscription sub = activeSubsMap != null ? activeSubsMap.get(user.getId()) : null;

        String currentPlanCode;
        String currentPlanName;
        Boolean isVip = false;
        LocalDateTime subscriptionEndDate = null;

        if (sub != null && sub.getMembershipPlan() != null) {
            MembershipPlan p = sub.getMembershipPlan();
            currentPlanCode = p.getPlanCode();
            currentPlanName = p.getName();
            subscriptionEndDate = sub.getEndDate();
            isVip = p.getPlanCode().toUpperCase().contains("VIP") || p.getPlanCode().toUpperCase().contains("PRO");
        } else {
            boolean isTeacher = roles.stream().anyMatch(r -> r.contains("TEACHER"));
            currentPlanCode = isTeacher ? "FREE_TEACHER" : "FREE_STUDENT";
            currentPlanName = isTeacher ? "Gói Giáo Viên Miễn Phí" : "Gói Học Sinh Miễn Phí";
            isVip = false;
        }

        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .roles(new HashSet<>(roles))
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .currentPlanCode(currentPlanCode)
                .currentPlanName(currentPlanName)
                .isVip(isVip)
                .subscriptionEndDate(subscriptionEndDate)
                .build();
    }
}
