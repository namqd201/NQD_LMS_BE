package com.nqd.nqd_lms_be.service;

import com.nqd.nqd_lms_be.dto.admin.AdminGrantVipRequest;
import com.nqd.nqd_lms_be.entity.MembershipPlan;
import com.nqd.nqd_lms_be.entity.Subscription;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.BillingCycle;
import com.nqd.nqd_lms_be.entity.enums.PlanUserType;
import com.nqd.nqd_lms_be.entity.enums.SubscriptionStatus;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse;
import com.nqd.nqd_lms_be.membership.service.MembershipPlanService;
import com.nqd.nqd_lms_be.membership.service.SubscriptionService;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.service.admin.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private MembershipPlanRepository membershipPlanRepository;
    @Mock
    private MembershipPlanService membershipPlanService;
    @Mock
    private EntitlementRepository entitlementRepository;
    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User teacherUser;
    private MembershipPlan teacherProPlan;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        teacherUser = User.builder()
                .email("teacher@test.com")
                .fullName("Teacher Test")
                .status(UserStatus.ACTIVE)
                .build();
        teacherUser.setId(userId);

        teacherProPlan = MembershipPlan.builder()
                .planCode("TEACHER_PRO_MONTHLY")
                .name("Gói Giáo Viên Pro (Tháng)")
                .userType(PlanUserType.TEACHER)
                .price(BigDecimal.valueOf(199000))
                .currency("VND")
                .billingCycle(BillingCycle.MONTHLY)
                .active(true)
                .features("[]")
                .build();
        teacherProPlan.setId(UUID.randomUUID());
        teacherProPlan.setIsDeleted(false);
    }

    @Test
    @DisplayName("grantUserVip resolves plan when given plan display name with brackets")
    void grantUserVip_resolvesPlanByNameMatch() {
        UUID userId = teacherUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(teacherUser));
        when(membershipPlanRepository.findByPlanCodeAndIsDeletedFalse("Gói Giáo Viên Pro (Tháng) ()"))
                .thenReturn(Optional.empty());
        when(membershipPlanRepository.findAll()).thenReturn(List.of(teacherProPlan));
        when(subscriptionRepository.findActiveSubscriptionsByUserList(eq(userId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        AdminGrantVipRequest request = AdminGrantVipRequest.builder()
                .planCode("Gói Giáo Viên Pro (Tháng) ()")
                .durationMonths(1)
                .reason("Test grant with display text")
                .build();

        SubscriptionResponse response = adminUserService.grantUserVip(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getPlanCode()).isEqualTo("TEACHER_PRO_MONTHLY");
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(entitlementRepository).save(any());
    }

    @Test
    @DisplayName("grantUserVip resolves plan when exact planCode is provided")
    void grantUserVip_resolvesPlanByExactCode() {
        UUID userId = teacherUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(teacherUser));
        when(membershipPlanRepository.findByPlanCodeAndIsDeletedFalse("TEACHER_PRO_MONTHLY"))
                .thenReturn(Optional.of(teacherProPlan));
        when(subscriptionRepository.findActiveSubscriptionsByUserList(eq(userId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        AdminGrantVipRequest request = AdminGrantVipRequest.builder()
                .planCode("TEACHER_PRO_MONTHLY")
                .durationMonths(3)
                .build();

        SubscriptionResponse response = adminUserService.grantUserVip(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getPlanCode()).isEqualTo("TEACHER_PRO_MONTHLY");
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }
}
