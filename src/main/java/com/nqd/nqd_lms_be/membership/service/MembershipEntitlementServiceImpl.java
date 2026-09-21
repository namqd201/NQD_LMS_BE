package com.nqd.nqd_lms_be.membership.service;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.LimitExceededException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.membership.dto.FeatureUsageDto;
import com.nqd.nqd_lms_be.membership.dto.UsageLimitCheckResult;
import com.nqd.nqd_lms_be.membership.dto.UsageStatusResponse;
import com.nqd.nqd_lms_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MembershipEntitlementServiceImpl implements MembershipEntitlementService {

    private final SubscriptionRepository subscriptionRepository;
    private final MembershipPlanService membershipPlanService;
    private final UserUsageRepository userUsageRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final QuestionRepository questionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final EntitlementRepository entitlementRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasFeature(UUID userId, FeatureKey feature) {
        if (userId == null || feature == null) return false;

        // 1. Admin has access to all features
        if (isUserAdmin(userId)) {
            return true;
        }

        // 2. Check active plan features
        MembershipPlan plan = getEffectivePlan(userId);
        if (plan != null && plan.hasFeature(feature)) {
            return true;
        }

        // 3. Check standalone active entitlements (e.g. course-specific or exam-specific purchases)
        if (feature == FeatureKey.PREMIUM_COURSES) {
            return entitlementRepository.existsByUserIdAndEntitlementTypeAndStatusAndIsDeletedFalse(
                    userId, EntitlementType.COURSE_ACCESS, EntitlementStatus.ACTIVE
            );
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public UsageLimitCheckResult checkLimit(UUID userId, FeatureKey feature) {
        if (userId == null || feature == null) {
            return UsageLimitCheckResult.exceeded(feature, 0, 0, "Người dùng hoặc tính năng không hợp lệ");
        }

        if (isUserAdmin(userId)) {
            return UsageLimitCheckResult.allowedUnlimited(feature, 0);
        }

        MembershipPlan plan = getEffectivePlan(userId);
        if (plan == null) {
            return UsageLimitCheckResult.exceeded(feature, 0, 0, "Không xác định được gói dịch vụ");
        }

        switch (feature) {
            case AI_TUTOR: {
                Integer limit = plan.getAiQuestionLimitPerDay();
                if (limit == null || limit < 0) {
                    return UsageLimitCheckResult.allowedUnlimited(feature, getDailyAiUsage(userId));
                }
                int currentUsage = getDailyAiUsage(userId);
                if (currentUsage >= limit) {
                    return UsageLimitCheckResult.exceeded(
                            feature, limit, currentUsage,
                            String.format("Bạn đã sử dụng hết %d/%d câu hỏi AI trong ngày. Vui lòng nâng cấp gói PRO để hỏi không giới hạn.", currentUsage, limit)
                    );
                }
                return UsageLimitCheckResult.allowedWithinLimit(feature, limit, currentUsage);
            }

            case PREMIUM_EXAMS:
            case EXAM_LIMIT: {
                Integer limit = plan.getExamLimitPerWeek();
                if (limit == null || limit < 0) {
                    return UsageLimitCheckResult.allowedUnlimited(feature, getWeeklyExamUsage(userId));
                }
                int currentUsage = getWeeklyExamUsage(userId);
                if (currentUsage >= limit) {
                    return UsageLimitCheckResult.exceeded(
                            feature, limit, currentUsage,
                            String.format("Bạn đã sử dụng hết %d/%d lượt thi trong tuần của gói Miễn phí. Vui lòng nâng cấp gói PRO để thi không giới hạn.", currentUsage, limit)
                    );
                }
                return UsageLimitCheckResult.allowedWithinLimit(feature, limit, currentUsage);
            }

            case CLASS_LIMIT: {
                Integer limit = plan.getMaxClassesLimit();
                int currentClasses = (int) courseRepository.countByCreatorIdAndIsDeletedFalse(userId);
                if (limit == null || limit < 0) {
                    return UsageLimitCheckResult.allowedUnlimited(feature, currentClasses);
                }
                if (currentClasses >= limit) {
                    return UsageLimitCheckResult.exceeded(
                            feature, limit, currentClasses,
                            String.format("Bạn đã tạo tối đa %d lớp/khóa học theo gói Giáo viên Miễn phí. Vui lòng nâng cấp Teacher Pro để tạo thêm.", limit)
                    );
                }
                return UsageLimitCheckResult.allowedWithinLimit(feature, limit, currentClasses);
            }

            case QUESTION_BANK_LIMIT: {
                Integer limit = plan.getMaxQuestionsLimit();
                int currentQuestions = (int) questionRepository.countByCreatorIdAndIsDeletedFalse(userId);
                if (limit == null || limit < 0) {
                    return UsageLimitCheckResult.allowedUnlimited(feature, currentQuestions);
                }
                if (currentQuestions >= limit) {
                    return UsageLimitCheckResult.exceeded(
                            feature, limit, currentQuestions,
                            String.format("Ngân hàng câu hỏi đã đạt giới hạn %d câu của gói Miễn phí. Vui lòng nâng cấp Teacher Pro để tạo không giới hạn.", limit)
                    );
                }
                return UsageLimitCheckResult.allowedWithinLimit(feature, limit, currentQuestions);
            }

            case ULTRA_COURSE_ENROLLMENT: {
                Integer dailyLimit = plan.getDailyCourseEnrollmentLimit() != null ? plan.getDailyCourseEnrollmentLimit() : 10;
                String todayKey = getTodayPeriodKey();
                int currentUsage = userUsageRepository.findByUserIdAndFeatureKeyAndPeriodKeyAndIsDeletedFalse(userId, feature, todayKey)
                        .map(r -> r.getUsageCount() != null ? r.getUsageCount() : 0)
                        .orElse(0);
                if (currentUsage >= dailyLimit) {
                    return UsageLimitCheckResult.exceeded(
                            feature, dailyLimit, currentUsage,
                            String.format("Bạn đã đạt giới hạn %d khóa học/ngày của gói Ultra. Vui lòng quay lại vào ngày mai.", dailyLimit)
                    );
                }
                return UsageLimitCheckResult.allowedWithinLimit(feature, dailyLimit, currentUsage);
            }

            case AI_EXAM_GENERATION:
            case ADVANCED_ANALYTICS:
            case PDF_DOWNLOAD:
            case VIDEO_HIGH_QUALITY: {
                if (plan.hasFeature(feature)) {
                    return UsageLimitCheckResult.allowedUnlimited(feature, 0);
                }
                return UsageLimitCheckResult.exceeded(
                        feature, 0, 0,
                        "Tính năng '" + feature.name() + "' yêu cầu gói dịch vụ VIP/Pro."
                );
            }

            default:
                return UsageLimitCheckResult.allowedUnlimited(feature, 0);
        }
    }

    @Override
    @Transactional
    public void enforceAndConsumeUsage(UUID userId, FeatureKey feature, int amount) {
        if (userId == null || isUserAdmin(userId)) {
            return;
        }

        MembershipPlan plan = getEffectivePlan(userId);
        if (plan == null) {
            throw new LimitExceededException("Không tìm thấy thông tin gói thành viên của người dùng.");
        }

        if (feature == FeatureKey.AI_TUTOR) {
            Integer limit = plan.getAiQuestionLimitPerDay();
            if (limit != null && limit >= 0) {
                String periodKey = getTodayPeriodKey();
                consumePeriodicUsageWithLock(userId, feature, UsagePeriodType.DAILY, periodKey, limit, amount);
            }
        } else if (feature == FeatureKey.PREMIUM_EXAMS || feature == FeatureKey.EXAM_LIMIT) {
            Integer limit = plan.getExamLimitPerWeek();
            if (limit != null && limit >= 0) {
                String periodKey = getWeekPeriodKey();
                consumePeriodicUsageWithLock(userId, FeatureKey.EXAM_LIMIT, UsagePeriodType.WEEKLY, periodKey, limit, amount);
            }
        } else if (feature == FeatureKey.CLASS_LIMIT) {
            Integer limit = plan.getMaxClassesLimit();
            if (limit != null && limit >= 0) {
                long currentCount = courseRepository.countByCreatorIdAndIsDeletedFalse(userId);
                if (currentCount + amount > limit) {
                    throw new LimitExceededException(
                            feature.name(), limit, (int) currentCount,
                            String.format("Bạn đã đạt giới hạn tối đa %d lớp/khóa học của gói Giáo viên Miễn phí. Nâng cấp Teacher Pro để tiếp tục.", limit)
                    );
                }
            }
        } else if (feature == FeatureKey.QUESTION_BANK_LIMIT) {
            Integer limit = plan.getMaxQuestionsLimit();
            if (limit != null && limit >= 0) {
                long currentCount = questionRepository.countByCreatorIdAndIsDeletedFalse(userId);
                if (currentCount + amount > limit) {
                    throw new LimitExceededException(
                            feature.name(), limit, (int) currentCount,
                            String.format("Ngân hàng câu hỏi đã đạt tối đa %d câu hỏi của gói Miễn phí. Nâng cấp Teacher Pro để tiếp tục.", limit)
                    );
                }
            }
        } else if (feature == FeatureKey.ULTRA_COURSE_ENROLLMENT) {
            Integer dailyLimit = plan.getDailyCourseEnrollmentLimit() != null ? plan.getDailyCourseEnrollmentLimit() : 10;
            Integer monthlyLimit = plan.getMonthlyCourseEnrollmentLimit() != null ? plan.getMonthlyCourseEnrollmentLimit() : 100;
            String todayKey = getTodayPeriodKey();
            String monthKey = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));

            // Check & consume daily (10/day)
            consumePeriodicUsageWithLock(userId, feature, UsagePeriodType.DAILY, todayKey, dailyLimit, amount);
            // Check & consume monthly (100/month)
            consumePeriodicUsageWithLock(userId, feature, UsagePeriodType.MONTHLY, monthKey, monthlyLimit, amount);
        } else {
            // For boolean flag features (AI_EXAM_GENERATION, etc.)
            enforceFeatureAccess(userId, feature);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void enforceFeatureAccess(UUID userId, FeatureKey feature) {
        if (!hasFeature(userId, feature)) {
            throw new ForbiddenOperationException(
                    "Tính năng '" + feature.name() + "' chỉ dành riêng cho tài khoản VIP / Pro. Vui lòng nâng cấp gói để trải nghiệm."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UsageStatusResponse getUsageStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        MembershipPlan plan = getEffectivePlan(userId);
        boolean isPremium = !plan.isFree();

        List<FeatureUsageDto> metrics = new ArrayList<>();

        // 1. AI Questions metric
        String todayKey = getTodayPeriodKey();
        int aiUsage = getDailyAiUsage(userId);
        Integer aiLimit = plan.getAiQuestionLimitPerDay();
        LocalDateTime tomorrowMidnight = LocalDate.now().plusDays(1).atStartOfDay();
        metrics.add(FeatureUsageDto.builder()
                .featureKey(FeatureKey.AI_TUTOR)
                .featureName("Câu hỏi AI Tutor (Hàng ngày)")
                .periodType(UsagePeriodType.DAILY)
                .periodKey(todayKey)
                .usageCount(aiUsage)
                .limit(aiLimit != null && aiLimit >= 0 ? aiLimit : -1)
                .remaining(aiLimit != null && aiLimit >= 0 ? Math.max(0, aiLimit - aiUsage) : null)
                .isExceeded(aiLimit != null && aiLimit >= 0 && aiUsage >= aiLimit)
                .resetsAt(tomorrowMidnight)
                .build());

        // 2. Exam metric
        String weekKey = getWeekPeriodKey();
        int examUsage = getWeeklyExamUsage(userId);
        Integer examLimit = plan.getExamLimitPerWeek();
        LocalDateTime nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay();
        metrics.add(FeatureUsageDto.builder()
                .featureKey(FeatureKey.EXAM_LIMIT)
                .featureName("Lượt làm bài thi (Hàng tuần)")
                .periodType(UsagePeriodType.WEEKLY)
                .periodKey(weekKey)
                .usageCount(examUsage)
                .limit(examLimit != null && examLimit >= 0 ? examLimit : -1)
                .remaining(examLimit != null && examLimit >= 0 ? Math.max(0, examLimit - examUsage) : null)
                .isExceeded(examLimit != null && examLimit >= 0 && examUsage >= examLimit)
                .resetsAt(nextMonday)
                .build());

        // 3. Teacher specific metrics if applicable
        if (plan.isTeacherPlan()) {
            int currentClasses = (int) courseRepository.countByCreatorIdAndIsDeletedFalse(userId);
            Integer classLimit = plan.getMaxClassesLimit();
            metrics.add(FeatureUsageDto.builder()
                    .featureKey(FeatureKey.CLASS_LIMIT)
                    .featureName("Số lượng lớp/khóa học")
                    .periodType(UsagePeriodType.TOTAL)
                    .periodKey("TOTAL")
                    .usageCount(currentClasses)
                    .limit(classLimit != null && classLimit >= 0 ? classLimit : -1)
                    .remaining(classLimit != null && classLimit >= 0 ? Math.max(0, classLimit - currentClasses) : null)
                    .isExceeded(classLimit != null && classLimit >= 0 && currentClasses >= classLimit)
                    .build());

            int currentQuestions = (int) questionRepository.countByCreatorIdAndIsDeletedFalse(userId);
            Integer questionLimit = plan.getMaxQuestionsLimit();
            metrics.add(FeatureUsageDto.builder()
                    .featureKey(FeatureKey.QUESTION_BANK_LIMIT)
                    .featureName("Số lượng câu hỏi ngân hàng")
                    .periodType(UsagePeriodType.TOTAL)
                    .periodKey("TOTAL")
                    .usageCount(currentQuestions)
                    .limit(questionLimit != null && questionLimit >= 0 ? questionLimit : -1)
                    .remaining(questionLimit != null && questionLimit >= 0 ? Math.max(0, questionLimit - currentQuestions) : null)
                    .isExceeded(questionLimit != null && questionLimit >= 0 && currentQuestions >= questionLimit)
                    .build());
        }

        return UsageStatusResponse.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userType(plan.getUserType())
                .currentPlanCode(plan.getPlanCode())
                .currentPlanName(plan.getName())
                .isPremium(isPremium)
                .activeFeatures(plan.getFeatureSet())
                .usageMetrics(metrics)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Subscription getEffectiveSubscription(UUID userId) {
        if (userId == null) return null;
        return subscriptionRepository.findActiveSubscriptionByUser(userId, LocalDateTime.now()).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipPlan getEffectivePlan(UUID userId) {
        if (userId == null) {
            return membershipPlanService.getPlanEntityByCode("FREE_STUDENT");
        }

        // 1. Check active subscription
        Subscription activeSub = getEffectiveSubscription(userId);
        if (activeSub != null && activeSub.getMembershipPlan() != null) {
            return activeSub.getMembershipPlan();
        }

        // 2. Check user roles to return baseline Free Plan
        if (isUserTeacher(userId)) {
            return membershipPlanService.getPlanEntityByCode("FREE_TEACHER");
        }

        return membershipPlanService.getPlanEntityByCode("FREE_STUDENT");
    }

    private void consumePeriodicUsageWithLock(
            UUID userId, FeatureKey featureKey, UsagePeriodType periodType,
            String periodKey, int limit, int amount
    ) {
        UserUsageRecord record = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            Optional<UserUsageRecord> opt = userUsageRepository.findByUserIdAndFeatureKeyAndPeriodKeyWithLock(userId, featureKey, periodKey);
            if (opt.isPresent()) {
                record = opt.get();
                break;
            }
            try {
                User user = userRepository.findById(userId).orElseThrow();
                UserUsageRecord newRec = UserUsageRecord.builder()
                        .user(user)
                        .featureKey(featureKey)
                        .periodType(periodType)
                        .periodKey(periodKey)
                        .usageCount(0)
                        .build();
                record = userUsageRepository.saveAndFlush(newRec);
                break;
            } catch (Exception ex) {
                log.debug("Concurrent insert collision for user usage record, retrying lock: {}", ex.getMessage());
            }
        }

        if (record == null) {
            record = userUsageRepository.findByUserIdAndFeatureKeyAndPeriodKeyWithLock(userId, featureKey, periodKey).orElseThrow();
        }

        int currentUsage = record.getUsageCount() != null ? record.getUsageCount() : 0;
        if (currentUsage + amount > limit) {
            log.warn("User {} exceeded limit for {}: current={}, requested={}, limit={}",
                    userId, featureKey, currentUsage, amount, limit);
            throw new LimitExceededException(
                    featureKey.name(), limit, currentUsage,
                    String.format("Bạn đã đạt giới hạn sử dụng %d lượt cho tính năng %s.", limit, featureKey.name())
            );
        }

        record.incrementUsage(amount);
        userUsageRepository.save(record);
        log.info("Consumed {} usage for user {}, feature: {}, new count: {}/{}",
                amount, userId, featureKey, record.getUsageCount(), limit);
    }

    private int getDailyAiUsage(UUID userId) {
        String todayKey = getTodayPeriodKey();
        return userUsageRepository.findByUserIdAndFeatureKeyAndPeriodKeyAndIsDeletedFalse(userId, FeatureKey.AI_TUTOR, todayKey)
                .map(r -> r.getUsageCount() != null ? r.getUsageCount() : 0)
                .orElse(0);
    }

    private int getWeeklyExamUsage(UUID userId) {
        String weekKey = getWeekPeriodKey();
        return userUsageRepository.findByUserIdAndFeatureKeyAndPeriodKeyAndIsDeletedFalse(userId, FeatureKey.EXAM_LIMIT, weekKey)
                .map(r -> r.getUsageCount() != null ? r.getUsageCount() : 0)
                .orElse(0);
    }

    private String getTodayPeriodKey() {
        return LocalDate.now().toString();
    }

    private String getWeekPeriodKey() {
        LocalDate now = LocalDate.now();
        int year = now.get(IsoFields.WEEK_BASED_YEAR);
        int week = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return String.format("%d-W%02d", year, week);
    }

    private boolean isUserAdmin(UUID userId) {
        List<UserRole> roles = userRoleRepository.findByUserId(userId);
        return roles.stream().anyMatch(ur -> {
            if (ur.getRole() != null && ur.getRole().getName() != null) {
                String roleName = ur.getRole().getName().toUpperCase();
                return roleName.contains("ADMIN");
            }
            if (ur.getRoleId() != null) {
                return roleRepository.findById(ur.getRoleId())
                        .map(r -> r.getName().toUpperCase().contains("ADMIN"))
                        .orElse(false);
            }
            return false;
        });
    }

    private boolean isUserTeacher(UUID userId) {
        List<UserRole> roles = userRoleRepository.findByUserId(userId);
        return roles.stream().anyMatch(ur -> {
            if (ur.getRole() != null && ur.getRole().getName() != null) {
                String roleName = ur.getRole().getName().toUpperCase();
                return roleName.contains("TEACHER");
            }
            if (ur.getRoleId() != null) {
                return roleRepository.findById(ur.getRoleId())
                        .map(r -> r.getName().toUpperCase().contains("TEACHER"))
                        .orElse(false);
            }
            return false;
        });
    }
}
