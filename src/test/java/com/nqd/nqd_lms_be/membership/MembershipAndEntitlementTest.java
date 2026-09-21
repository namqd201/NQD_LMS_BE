package com.nqd.nqd_lms_be.membership;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.LimitExceededException;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.dto.admin.AdminGrantVipRequest;
import com.nqd.nqd_lms_be.membership.dto.MembershipPlanResponse;
import com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse;
import com.nqd.nqd_lms_be.membership.dto.UsageLimitCheckResult;
import com.nqd.nqd_lms_be.membership.dto.UsageStatusResponse;
import com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService;
import com.nqd.nqd_lms_be.membership.service.MembershipPlanService;
import com.nqd.nqd_lms_be.membership.service.SubscriptionService;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.service.admin.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MembershipAndEntitlementTest {

    @Autowired
    private MembershipPlanService membershipPlanService;

    @Autowired
    private MembershipEntitlementService membershipEntitlementService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserUsageRepository userUsageRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    private User student;
    private User teacher;
    private Role studentRole;
    private Role teacherRole;
    private Subject subject;

    @BeforeEach
    void setUp() {
        studentRole = roleRepository.findByName("STUDENT").orElseGet(() ->
                roleRepository.save(Role.builder().name("STUDENT").description("Student").build())
        );

        teacherRole = roleRepository.findByName("TEACHER").orElseGet(() ->
                roleRepository.save(Role.builder().name("TEACHER").description("Teacher").build())
        );

        student = userRepository.save(User.builder()
                .email("student_test_" + System.currentTimeMillis() + "@test.com")
                .fullName("Test Student")
                .status(UserStatus.ACTIVE)
                .build());

        userRoleRepository.save(UserRole.builder()
                .userId(student.getId())
                .roleId(studentRole.getId())
                .user(student)
                .role(studentRole)
                .build());

        teacher = userRepository.save(User.builder()
                .email("teacher_test_" + System.currentTimeMillis() + "@test.com")
                .fullName("Test Teacher")
                .status(UserStatus.ACTIVE)
                .build());

        userRoleRepository.save(UserRole.builder()
                .userId(teacher.getId())
                .roleId(teacherRole.getId())
                .user(teacher)
                .role(teacherRole)
                .build());

        subject = subjectRepository.findAll().stream().findFirst().orElseGet(() ->
                subjectRepository.save(Subject.builder().name("Toán").code("MATH_" + System.currentTimeMillis()).build())
        );
    }

    @Test
    @DisplayName("1. Default baseline plans are initialized and active")
    void testDefaultPlansInitialization() {
        List<MembershipPlanResponse> studentPlans = membershipPlanService.getPublicPlans(PlanUserType.STUDENT);
        assertThat(studentPlans).isNotEmpty();
        assertThat(studentPlans).anyMatch(p -> "FREE_STUDENT".equals(p.getPlanCode()));
        assertThat(studentPlans).anyMatch(p -> "VIP_STUDENT_MONTHLY".equals(p.getPlanCode()));
        assertThat(studentPlans).anyMatch(p -> "VIP_STUDENT_YEARLY".equals(p.getPlanCode()));

        List<MembershipPlanResponse> teacherPlans = membershipPlanService.getPublicPlans(PlanUserType.TEACHER);
        assertThat(teacherPlans).isNotEmpty();
        assertThat(teacherPlans).anyMatch(p -> "FREE_TEACHER".equals(p.getPlanCode()));
        assertThat(teacherPlans).anyMatch(p -> "TEACHER_PRO_MONTHLY".equals(p.getPlanCode()));
        assertThat(teacherPlans).anyMatch(p -> "TEACHER_PRO_YEARLY".equals(p.getPlanCode()));
    }

    @Test
    @DisplayName("2. Free Student has 5 AI questions/day limit and is enforced")
    void testFreeStudentAiQuestionsLimit() {
        // Free student can ask up to 5 questions
        for (int i = 1; i <= 5; i++) {
            UsageLimitCheckResult check = membershipEntitlementService.checkLimit(student.getId(), FeatureKey.AI_TUTOR);
            assertTrue(check.isAllowed());
            membershipEntitlementService.enforceAndConsumeUsage(student.getId(), FeatureKey.AI_TUTOR, 1);
        }

        // 6th question should be rejected with LimitExceededException
        UsageLimitCheckResult checkExceeded = membershipEntitlementService.checkLimit(student.getId(), FeatureKey.AI_TUTOR);
        assertFalse(checkExceeded.isAllowed());

        assertThrows(LimitExceededException.class, () ->
                membershipEntitlementService.enforceAndConsumeUsage(student.getId(), FeatureKey.AI_TUTOR, 1)
        );
    }

    @Test
    @DisplayName("3. Free Student has 3 exams/week limit and is enforced")
    void testFreeStudentWeeklyExamLimit() {
        for (int i = 1; i <= 3; i++) {
            UsageLimitCheckResult check = membershipEntitlementService.checkLimit(student.getId(), FeatureKey.EXAM_LIMIT);
            assertTrue(check.isAllowed());
            membershipEntitlementService.enforceAndConsumeUsage(student.getId(), FeatureKey.EXAM_LIMIT, 1);
        }

        UsageLimitCheckResult checkExceeded = membershipEntitlementService.checkLimit(student.getId(), FeatureKey.EXAM_LIMIT);
        assertFalse(checkExceeded.isAllowed());

        assertThrows(LimitExceededException.class, () ->
                membershipEntitlementService.enforceAndConsumeUsage(student.getId(), FeatureKey.EXAM_LIMIT, 1)
        );
    }

    @Test
    @DisplayName("4. VIP Student has unlimited AI questions and exams")
    void testVipStudentUnlimitedUsage() {
        MembershipPlan vipPlan = membershipPlanService.getPlanEntityByCode("VIP_STUDENT_MONTHLY");
        subscriptionService.activateOrUpgradeSubscription(student.getId(), vipPlan.getId(), null, true);

        // Can consume 10 questions without exceeding limit
        for (int i = 1; i <= 10; i++) {
            membershipEntitlementService.enforceAndConsumeUsage(student.getId(), FeatureKey.AI_TUTOR, 1);
        }

        UsageLimitCheckResult check = membershipEntitlementService.checkLimit(student.getId(), FeatureKey.AI_TUTOR);
        assertTrue(check.isAllowed());
        assertEquals(-1, check.getLimit());
    }

    @Test
    @DisplayName("5. Free Teacher is blocked from AI Exam Generation; Pro Teacher has access")
    void testTeacherAiGenerationEntitlement() {
        // Free Teacher does not have AI_EXAM_GENERATION
        assertFalse(membershipEntitlementService.hasFeature(teacher.getId(), FeatureKey.AI_EXAM_GENERATION));
        assertThrows(ForbiddenOperationException.class, () ->
                membershipEntitlementService.enforceFeatureAccess(teacher.getId(), FeatureKey.AI_EXAM_GENERATION)
        );

        // Upgrade Teacher to Teacher Pro
        MembershipPlan proPlan = membershipPlanService.getPlanEntityByCode("TEACHER_PRO_MONTHLY");
        subscriptionService.activateOrUpgradeSubscription(teacher.getId(), proPlan.getId(), null, true);

        // Now Pro Teacher has AI_EXAM_GENERATION
        assertTrue(membershipEntitlementService.hasFeature(teacher.getId(), FeatureKey.AI_EXAM_GENERATION));
        assertDoesNotThrow(() ->
                membershipEntitlementService.enforceFeatureAccess(teacher.getId(), FeatureKey.AI_EXAM_GENERATION)
        );
    }

    @Test
    @DisplayName("6. Subscription lifecycle: Upgrade, Cancel Auto-Renew, and Expiration sweep")
    void testSubscriptionLifecycle() {
        MembershipPlan vipPlan = membershipPlanService.getPlanEntityByCode("VIP_STUDENT_MONTHLY");
        SubscriptionResponse subResponse = subscriptionService.activateOrUpgradeSubscription(
                student.getId(), vipPlan.getId(), null, true
        );

        assertNotNull(subResponse);
        assertTrue(subResponse.isCurrentlyActive());
        assertTrue(subResponse.getAutoRenew());

        // Cancel Auto-Renew
        SubscriptionResponse cancelledResponse = subscriptionService.cancelAutoRenew(student.getId(), subResponse.getId());
        assertFalse(cancelledResponse.getAutoRenew());
        assertNotNull(cancelledResponse.getCancelledAt());
        // Access is STILL active until endDate
        assertTrue(cancelledResponse.isCurrentlyActive());

        // Simulate expired subscription
        Subscription subEntity = subscriptionRepository.findById(subResponse.getId()).orElseThrow();
        subEntity.setEndDate(LocalDateTime.now().minusDays(1));
        subscriptionRepository.save(subEntity);

        // Sweep expired subscriptions
        subscriptionService.expirePastDueSubscriptions();

        Subscription expiredSub = subscriptionRepository.findById(subResponse.getId()).orElseThrow();
        assertEquals(SubscriptionStatus.EXPIRED, expiredSub.getStatus());

        // Effective plan immediately falls back to FREE_STUDENT
        MembershipPlan effectivePlan = membershipEntitlementService.getEffectivePlan(student.getId());
        assertEquals("FREE_STUDENT", effectivePlan.getPlanCode());
    }

    @Test
    @DisplayName("7. Concurrency safe usage consumption prevents race conditions")
    void testConcurrentUsageConsumption() throws InterruptedException {
        // Pre-consume 1 to initialize the record
        membershipEntitlementService.enforceAndConsumeUsage(student.getId(), FeatureKey.AI_TUTOR, 1);

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    membershipEntitlementService.enforceAndConsumeUsage(student.getId(), FeatureKey.AI_TUTOR, 1);
                    successCount.incrementAndGet();
                } catch (LimitExceededException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected error
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Trigger all threads at once
        endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly 4 should succeed (1 + 4 = 5 total quota) and 6 should fail due to pessimistic row locking
        assertEquals(4, successCount.get());
        assertEquals(6, failureCount.get());
    }

    @Test
    @DisplayName("8. Usage status returns detailed metrics for Student")
    void testUsageStatusMetrics() {
        membershipEntitlementService.enforceAndConsumeUsage(student.getId(), FeatureKey.AI_TUTOR, 2);

        UsageStatusResponse status = membershipEntitlementService.getUsageStatus(student.getId());
        assertNotNull(status);
        assertEquals("FREE_STUDENT", status.getCurrentPlanCode());
        assertFalse(status.isPremium());
        assertThat(status.getUsageMetrics()).isNotEmpty();

        assertThat(status.getUsageMetrics()).anyMatch(m ->
                m.getFeatureKey() == FeatureKey.AI_TUTOR && m.getUsageCount() == 2 && m.getRemaining() == 3
        );
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
    @DisplayName("9. Admin can grant and revoke VIP/PRO directly to student and teacher free of charge")
    void testAdminGrantAndRevokeVip() {
        // 1. Initially student is FREE_STUDENT
        MembershipPlan initialPlan = membershipEntitlementService.getEffectivePlan(student.getId());
        assertEquals("FREE_STUDENT", initialPlan.getPlanCode());

        // 2. Admin grants VIP (1 month)
        AdminGrantVipRequest grantReq = AdminGrantVipRequest.builder()
                .planCode("VIP_STUDENT_MONTHLY")
                .durationMonths(1)
                .reason("Tài khoản người thân Admin")
                .build();

        SubscriptionResponse subResp = adminUserService.grantUserVip(student.getId(), grantReq);
        assertNotNull(subResp);
        assertEquals("VIP_STUDENT_MONTHLY", subResp.getPlanCode());
        assertTrue(subResp.isCurrentlyActive());

        // Effective plan is now VIP
        MembershipPlan upgradedPlan = membershipEntitlementService.getEffectivePlan(student.getId());
        assertEquals("VIP_STUDENT_MONTHLY", upgradedPlan.getPlanCode());
        assertTrue(upgradedPlan.hasFeature(FeatureKey.PDF_DOWNLOAD));

        // 3. Admin grants Teacher PRO Lifetime (-1)
        AdminGrantVipRequest grantTeacherReq = AdminGrantVipRequest.builder()
                .planCode("TEACHER_PRO_YEARLY")
                .durationMonths(-1) // Lifetime
                .reason("Giáo viên test dự án")
                .build();

        SubscriptionResponse teacherSubResp = adminUserService.grantUserVip(teacher.getId(), grantTeacherReq);
        assertNotNull(teacherSubResp);
        assertEquals("TEACHER_PRO_YEARLY", teacherSubResp.getPlanCode());
        assertTrue(teacherSubResp.getEndDate().isAfter(LocalDateTime.now().plusYears(50)));

        // 4. Admin revokes VIP from student -> falls back to FREE_STUDENT
        SubscriptionResponse revokedResp = adminUserService.revokeUserVip(student.getId(), "Hết hạn thử nghiệm");
        assertEquals("FREE_STUDENT", revokedResp.getPlanCode());

        MembershipPlan fallbackPlan = membershipEntitlementService.getEffectivePlan(student.getId());
        assertEquals("FREE_STUDENT", fallbackPlan.getPlanCode());
    }
}
