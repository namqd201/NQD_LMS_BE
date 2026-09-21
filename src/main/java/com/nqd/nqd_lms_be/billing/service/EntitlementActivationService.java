package com.nqd.nqd_lms_be.billing.service;

import com.nqd.nqd_lms_be.billing.notification.BillingNotificationService;
import com.nqd.nqd_lms_be.billing.notification.PaymentSuccessNotificationEvent;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EntitlementActivationService {

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final EntitlementRepository entitlementRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final BillingNotificationService billingNotificationService;
    private final com.nqd.nqd_lms_be.finance.service.TeacherFinanceService teacherFinanceService;

    /**
     * Atomically and idempotently activates entitlements, course enrollments, and subscriptions for a paid order.
     */
    @Transactional
    public void activateOrderFulfillment(Order order) {
        if (order == null || order.getUser() == null) {
            log.warn("Cannot activate fulfillment: Order or User is null");
            return;
        }

        User user = order.getUser();
        log.info("Starting fulfillment activation for order #{}, user: {}", order.getOrderCode(), user.getEmail());

        List<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            log.warn("Order #{} has no line items to fulfill", order.getOrderCode());
            return;
        }

        String primaryProductTitle = items.get(0).getProductTitle();
        String primaryAccessUrl = "/student/courses";

        for (OrderItem item : items) {
            Product product = item.getProduct();
            ProductType type = item.getProductType();

            if (type != null && type.isCourse()) {
                primaryAccessUrl = fulfillCourseAccess(order, user, item, product);
            } else if (type != null && type.isChapter()) {
                primaryAccessUrl = fulfillChapterAccess(order, user, item, product);
            } else if (type != null && type.isLesson()) {
                primaryAccessUrl = fulfillLessonAccess(order, user, item, product);
            } else if (type != null && type.isMembership()) {
                primaryAccessUrl = fulfillMembershipSubscription(order, user, item, product);
            } else {
                fulfillGenericEntitlement(order, user, item, product);
            }
        }

        // Send payment success & access instructions notification
        PaymentSuccessNotificationEvent notificationEvent = PaymentSuccessNotificationEvent.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userFullName(user.getFullName())
                .orderCode(order.getOrderCode())
                .productTitle(primaryProductTitle)
                .amount(order.getFinalAmount())
                .currency(order.getCurrency())
                .paymentStatus("PAID")
                .paidAt(order.getCompletedAt() != null ? order.getCompletedAt() : LocalDateTime.now())
                .accessUrl(primaryAccessUrl)
                .instructions("Truy cập trang Khóa học của tôi để bắt đầu học ngay.")
                .build();

        billingNotificationService.sendPaymentSuccessNotification(notificationEvent);
        log.info("Fulfillment activation completed successfully for order #{}", order.getOrderCode());
    }

    private String fulfillCourseAccess(Order order, User user, OrderItem item, Product product) {
        UUID courseId = product != null ? product.getTargetEntityId() : null;
        if (courseId == null) {
            log.warn("Course product {} has no targetEntityId (courseId)", item.getProductTitle());
            return "/student/courses";
        }

        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            log.warn("Course with ID {} not found during order activation", courseId);
            return "/student/courses";
        }

        Course course = courseOpt.get();

        // 1. Idempotent CourseEnrollment creation/activation
        Optional<CourseEnrollment> existingEnrollment = courseEnrollmentRepository.findByCourseIdAndStudentId(courseId, user.getId());
        if (existingEnrollment.isEmpty()) {
            CourseEnrollment enrollment = CourseEnrollment.builder()
                    .course(course)
                    .student(user)
                    .status(EnrollmentStatus.ENROLLED)
                    .enrolledAt(LocalDateTime.now())
                    .build();
            courseEnrollmentRepository.save(enrollment);
            log.info("Created CourseEnrollment for student {} in course {}", user.getEmail(), course.getName());
        } else {
            CourseEnrollment enrollment = existingEnrollment.get();
            if (enrollment.getStatus() != EnrollmentStatus.ENROLLED) {
                enrollment.setStatus(EnrollmentStatus.ENROLLED);
                courseEnrollmentRepository.save(enrollment);
            }
        }

        // 2. Idempotent Entitlement record
        Optional<Entitlement> existingEntitlement = entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                user.getId(), EntitlementType.COURSE_ACCESS, courseId, EntitlementStatus.ACTIVE
        );

        if (existingEntitlement.isEmpty()) {
            Entitlement entitlement = Entitlement.builder()
                    .user(user)
                    .product(product)
                    .entitlementType(EntitlementType.COURSE_ACCESS)
                    .targetEntityId(courseId)
                    .status(EntitlementStatus.ACTIVE)
                    .validFrom(LocalDateTime.now())
                    .validUntil(null) // Lifetime access for single course purchase unless configured otherwise
                    .sourceOrder(order)
                    .build();
            entitlementRepository.save(entitlement);
            log.info("Created Entitlement COURSE_ACCESS for student {} on course {}", user.getEmail(), courseId);
        }

        // 3. Record Teacher Earning (Idempotent & Snapshot-based)
        try {
            teacherFinanceService.recordEarningForOrderItem(item);
        } catch (Exception ex) {
            log.error("Failed to record teacher earning for order item {}: {}", item.getId(), ex.getMessage(), ex);
        }

        return "/student/courses/" + courseId;
    }

    private String fulfillMembershipSubscription(Order order, User user, OrderItem item, Product product) {
        UUID planId = product != null ? product.getTargetEntityId() : null;
        Optional<MembershipPlan> planOpt = planId != null ? membershipPlanRepository.findById(planId) : Optional.empty();

        MembershipPlan plan;
        if (planOpt.isPresent()) {
            plan = planOpt.get();
        } else {
            // Find by product code or fallback to standard plan
            plan = membershipPlanRepository.findAll().stream().findFirst().orElse(null);
        }

        if (plan == null) {
            log.warn("No MembershipPlan found for product {}", item.getProductTitle());
            return "/pricing";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now;
        LocalDateTime endDate = null;

        // 1. Supersede or extend active subscriptions
        List<Subscription> existingActiveSubs = subscriptionRepository.findActiveSubscriptionsByUserWithLock(user.getId());
        if (!existingActiveSubs.isEmpty()) {
            Subscription currentSub = existingActiveSubs.get(0);
            if (currentSub.getMembershipPlan().getId().equals(plan.getId()) && currentSub.getEndDate() != null && currentSub.getEndDate().isAfter(now)) {
                startDate = currentSub.getEndDate();
            } else {
                for (Subscription oldSub : existingActiveSubs) {
                    oldSub.setStatus(SubscriptionStatus.CANCELLED);
                    oldSub.setCancelledAt(now);
                    subscriptionRepository.save(oldSub);
                }
            }
        }

        if (plan.getBillingCycle() == BillingCycle.MONTHLY) {
            endDate = startDate.plusMonths(1);
        } else if (plan.getBillingCycle() == BillingCycle.QUARTERLY) {
            endDate = startDate.plusMonths(3);
        } else if (plan.getBillingCycle() == BillingCycle.YEARLY) {
            endDate = startDate.plusYears(1);
        }

        Subscription subscription = Subscription.builder()
                .user(user)
                .membershipPlan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(startDate.isBefore(now) ? now : startDate)
                .endDate(endDate)
                .autoRenew(true)
                .sourceOrder(order)
                .build();
        subscription = subscriptionRepository.save(subscription);

        // 2. Core Membership Benefit Entitlement
        Entitlement entitlement = Entitlement.builder()
                .user(user)
                .product(product)
                .entitlementType(EntitlementType.MEMBERSHIP_BENEFIT)
                .targetEntityId(plan.getId())
                .status(EntitlementStatus.ACTIVE)
                .validFrom(subscription.getStartDate())
                .validUntil(subscription.getEndDate())
                .sourceOrder(order)
                .sourceSubscription(subscription)
                .build();
        entitlementRepository.save(entitlement);

        // 3. AI Tutor Unlimited Entitlement
        if (plan.hasFeature(FeatureKey.AI_TUTOR) || (plan.getAiQuestionLimitPerDay() != null && plan.getAiQuestionLimitPerDay() < 0)) {
            Entitlement aiEntitlement = Entitlement.builder()
                    .user(user)
                    .product(product)
                    .entitlementType(EntitlementType.AI_UNLIMITED)
                    .targetEntityId(plan.getId())
                    .status(EntitlementStatus.ACTIVE)
                    .validFrom(subscription.getStartDate())
                    .validUntil(subscription.getEndDate())
                    .sourceOrder(order)
                    .sourceSubscription(subscription)
                    .build();
            entitlementRepository.save(aiEntitlement);
        }

        // 4. Exam Access Entitlement
        if (plan.hasFeature(FeatureKey.PREMIUM_EXAMS) || (plan.getExamLimitPerWeek() != null && plan.getExamLimitPerWeek() < 0)) {
            Entitlement examEntitlement = Entitlement.builder()
                    .user(user)
                    .product(product)
                    .entitlementType(EntitlementType.EXAM_ACCESS)
                    .targetEntityId(plan.getId())
                    .status(EntitlementStatus.ACTIVE)
                    .validFrom(subscription.getStartDate())
                    .validUntil(subscription.getEndDate())
                    .sourceOrder(order)
                    .sourceSubscription(subscription)
                    .build();
            entitlementRepository.save(examEntitlement);
        }

        log.info("Activated Subscription and all Entitlements for plan {} for user {}", plan.getName(), user.getEmail());
        return "/";
    }

    private String fulfillChapterAccess(Order order, User user, OrderItem item, Product product) {
        UUID chapterId = product != null ? product.getTargetEntityId() : null;
        if (chapterId == null) {
            return "/student/courses";
        }

        Optional<Entitlement> existingEntitlement = entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                user.getId(), EntitlementType.CHAPTER_ACCESS, chapterId, EntitlementStatus.ACTIVE
        );

        if (existingEntitlement.isEmpty()) {
            Entitlement entitlement = Entitlement.builder()
                    .user(user)
                    .product(product)
                    .entitlementType(EntitlementType.CHAPTER_ACCESS)
                    .targetEntityId(chapterId)
                    .status(EntitlementStatus.ACTIVE)
                    .validFrom(LocalDateTime.now())
                    .validUntil(null)
                    .sourceOrder(order)
                    .build();
            entitlementRepository.save(entitlement);
            log.info("Created Entitlement CHAPTER_ACCESS for student {} on chapter {}", user.getEmail(), chapterId);
        }

        try {
            teacherFinanceService.recordEarningForOrderItem(item);
        } catch (Exception ex) {
            log.error("Failed to record teacher earning for chapter item {}: {}", item.getId(), ex.getMessage(), ex);
        }

        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        return chapter != null && chapter.getCourse() != null
                ? "/student/courses/" + chapter.getCourse().getId()
                : "/student/courses";
    }

    private String fulfillLessonAccess(Order order, User user, OrderItem item, Product product) {
        UUID lessonId = product != null ? product.getTargetEntityId() : null;
        if (lessonId == null) {
            return "/student/courses";
        }

        Optional<Entitlement> existingEntitlement = entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                user.getId(), EntitlementType.LESSON_ACCESS, lessonId, EntitlementStatus.ACTIVE
        );

        if (existingEntitlement.isEmpty()) {
            Entitlement entitlement = Entitlement.builder()
                    .user(user)
                    .product(product)
                    .entitlementType(EntitlementType.LESSON_ACCESS)
                    .targetEntityId(lessonId)
                    .status(EntitlementStatus.ACTIVE)
                    .validFrom(LocalDateTime.now())
                    .validUntil(null)
                    .sourceOrder(order)
                    .build();
            entitlementRepository.save(entitlement);
            log.info("Created Entitlement LESSON_ACCESS for student {} on lesson {}", user.getEmail(), lessonId);
        }

        try {
            teacherFinanceService.recordEarningForOrderItem(item);
        } catch (Exception ex) {
            log.error("Failed to record teacher earning for lesson item {}: {}", item.getId(), ex.getMessage(), ex);
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        return lesson != null && lesson.getChapter() != null && lesson.getChapter().getCourse() != null
                ? "/student/courses/" + lesson.getChapter().getCourse().getId()
                : "/student/courses";
    }

    private void fulfillGenericEntitlement(Order order, User user, OrderItem item, Product product) {
        UUID targetId = product != null ? product.getTargetEntityId() : null;
        Entitlement entitlement = Entitlement.builder()
                .user(user)
                .product(product)
                .entitlementType(EntitlementType.COURSE_ACCESS)
                .targetEntityId(targetId)
                .status(EntitlementStatus.ACTIVE)
                .validFrom(LocalDateTime.now())
                .sourceOrder(order)
                .build();
        entitlementRepository.save(entitlement);
    }
}
