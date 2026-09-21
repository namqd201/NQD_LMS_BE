package com.nqd.nqd_lms_be.membership.service;

import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.entity.MembershipPlan;
import com.nqd.nqd_lms_be.entity.enums.BillingCycle;
import com.nqd.nqd_lms_be.entity.enums.PlanUserType;
import com.nqd.nqd_lms_be.entity.enums.ProductStatus;
import com.nqd.nqd_lms_be.membership.dto.CreateMembershipPlanRequest;
import com.nqd.nqd_lms_be.membership.dto.MembershipPlanResponse;
import com.nqd.nqd_lms_be.membership.dto.UpdateMembershipPlanRequest;
import com.nqd.nqd_lms_be.repository.MembershipPlanRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MembershipPlanServiceImpl implements MembershipPlanService {

    private final MembershipPlanRepository membershipPlanRepository;

    @PostConstruct
    public void init() {
        try {
            initDefaultPlans();
        } catch (Exception e) {
            log.error("Failed to seed default membership plans on startup: ", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipPlanResponse> getPublicPlans(PlanUserType userType) {
        List<MembershipPlan> plans;
        if (userType == null || userType == PlanUserType.ALL) {
            plans = membershipPlanRepository.findByStatusAndIsDeletedFalseOrderByPriceAsc(ProductStatus.PUBLISHED);
        } else {
            plans = membershipPlanRepository.findByUserTypeInAndStatusAndActiveTrueAndIsDeletedFalseOrderByPriceAsc(
                    List.of(userType, PlanUserType.ALL), ProductStatus.PUBLISHED
            );
        }
        return plans.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .map(MembershipPlanResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipPlanResponse> getAllPlansAdmin() {
        return membershipPlanRepository.findAll().stream()
                .filter(p -> !p.isDeleted())
                .map(MembershipPlanResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipPlanResponse getPlanById(UUID planId) {
        return MembershipPlanResponse.fromEntity(getPlanEntityById(planId));
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipPlan getPlanEntityById(UUID planId) {
        return membershipPlanRepository.findById(planId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói thành viên với ID: " + planId));
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipPlan getPlanEntityByCode(String planCode) {
        return membershipPlanRepository.findByPlanCodeAndIsDeletedFalse(planCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói thành viên với mã: " + planCode));
    }

    @Override
    @Transactional
    public MembershipPlanResponse createPlan(CreateMembershipPlanRequest request) {
        if (membershipPlanRepository.existsByPlanCode(request.getPlanCode())) {
            throw new IllegalArgumentException("Mã gói thành viên '" + request.getPlanCode() + "' đã tồn tại trên hệ thống.");
        }

        String featuresStr = request.getFeatures() != null ? String.join(",", request.getFeatures()) : "";

        MembershipPlan plan = MembershipPlan.builder()
                .planCode(request.getPlanCode().trim().toUpperCase())
                .name(request.getName().trim())
                .userType(request.getUserType())
                .description(request.getDescription())
                .price(request.getPrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .billingCycle(request.getBillingCycle())
                .active(request.getActive() != null ? request.getActive() : true)
                .examLimitPerWeek(request.getExamLimitPerWeek())
                .aiQuestionLimitPerDay(request.getAiQuestionLimitPerDay())
                .maxClassesLimit(request.getMaxClassesLimit())
                .maxQuestionsLimit(request.getMaxQuestionsLimit())
                .aiPromptLimitPerMonth(request.getAiPromptLimitPerMonth() != null ? request.getAiPromptLimitPerMonth() : 100)
                .examCreationLimit(request.getExamCreationLimit() != null ? request.getExamCreationLimit() : 50)
                .features(featuresStr)
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.PUBLISHED)
                .build();

        MembershipPlan saved = membershipPlanRepository.save(plan);
        log.info("Created new MembershipPlan: {} ({}) for userType {}", saved.getName(), saved.getPlanCode(), saved.getUserType());
        return MembershipPlanResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public MembershipPlanResponse updatePlan(UUID planId, UpdateMembershipPlanRequest request) {
        MembershipPlan plan = getPlanEntityById(planId);

        if (request.getName() != null) plan.setName(request.getName().trim());
        if (request.getUserType() != null) plan.setUserType(request.getUserType());
        if (request.getDescription() != null) plan.setDescription(request.getDescription());
        if (request.getPrice() != null) plan.setPrice(request.getPrice());
        if (request.getCurrency() != null) plan.setCurrency(request.getCurrency());
        if (request.getBillingCycle() != null) plan.setBillingCycle(request.getBillingCycle());
        if (request.getActive() != null) plan.setActive(request.getActive());
        if (request.getExamLimitPerWeek() != null) plan.setExamLimitPerWeek(request.getExamLimitPerWeek());
        if (request.getAiQuestionLimitPerDay() != null) plan.setAiQuestionLimitPerDay(request.getAiQuestionLimitPerDay());
        if (request.getMaxClassesLimit() != null) plan.setMaxClassesLimit(request.getMaxClassesLimit());
        if (request.getMaxQuestionsLimit() != null) plan.setMaxQuestionsLimit(request.getMaxQuestionsLimit());
        if (request.getAiPromptLimitPerMonth() != null) plan.setAiPromptLimitPerMonth(request.getAiPromptLimitPerMonth());
        if (request.getExamCreationLimit() != null) plan.setExamCreationLimit(request.getExamCreationLimit());
        if (request.getFeatures() != null) plan.setFeatures(String.join(",", request.getFeatures()));
        if (request.getStatus() != null) plan.setStatus(request.getStatus());

        MembershipPlan saved = membershipPlanRepository.save(plan);
        log.info("Updated MembershipPlan: {} ({})", saved.getName(), saved.getPlanCode());
        return MembershipPlanResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public MembershipPlanResponse togglePlanStatus(UUID planId, boolean active) {
        MembershipPlan plan = getPlanEntityById(planId);
        plan.setActive(active);
        MembershipPlan saved = membershipPlanRepository.save(plan);
        log.info("Toggled MembershipPlan {} active status to {}", plan.getPlanCode(), active);
        return MembershipPlanResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void initDefaultPlans() {
        // Migration helper: If VIP plans exist, upgrade them to PRO
        membershipPlanRepository.findByPlanCodeAndIsDeletedFalse("VIP_STUDENT_MONTHLY").ifPresent(plan -> {
            plan.setPlanCode("PRO_STUDENT_MONTHLY");
            plan.setName("Gói Học Sinh Pro (Tháng)");
            plan.setFeatures(plan.getFeatures() + ",DISCOUNT_ON_PURCHASES");
            membershipPlanRepository.save(plan);
        });
        membershipPlanRepository.findByPlanCodeAndIsDeletedFalse("VIP_STUDENT_YEARLY").ifPresent(plan -> {
            plan.setPlanCode("PRO_STUDENT_YEARLY");
            plan.setName("Gói Học Sinh Pro (Năm)");
            plan.setFeatures(plan.getFeatures() + ",DISCOUNT_ON_PURCHASES");
            membershipPlanRepository.save(plan);
        });

        // Upgrade FREE_STUDENT limits to 7 exams/week and 10 AI questions/day
        membershipPlanRepository.findByPlanCodeAndIsDeletedFalse("FREE_STUDENT").ifPresent(plan -> {
            plan.setExamLimitPerWeek(7);
            plan.setAiQuestionLimitPerDay(10);
            plan.setDescription("Gói cơ bản trải nghiệm nền tảng học trực tuyến với 7 đề thi/tuần và 10 câu hỏi AI/ngày.");
            membershipPlanRepository.save(plan);
        });

        // Upgrade FREE_TEACHER limits to 5 classes and 100 questions
        membershipPlanRepository.findByPlanCodeAndIsDeletedFalse("FREE_TEACHER").ifPresent(plan -> {
            plan.setMaxClassesLimit(5);
            plan.setMaxQuestionsLimit(100);
            plan.setDescription("Gói giáo viên cơ bản: Tối đa 5 lớp/khóa học và 100 câu hỏi trong ngân hàng câu hỏi.");
            membershipPlanRepository.save(plan);
        });

        // 1. Student Plans
        createDefaultPlanIfAbsent(
                "FREE_STUDENT",
                "Gói Học Sinh Miễn Phí",
                PlanUserType.STUDENT,
                "Gói cơ bản trải nghiệm nền tảng học trực tuyến với 7 đề thi/tuần và 10 câu hỏi AI/ngày.",
                BigDecimal.ZERO,
                BillingCycle.LIFETIME,
                7,  // 7 exams/week
                10, // 10 AI questions/day
                null, null, null, null,
                "AI_TUTOR,PREMIUM_COURSES"
        );

        createDefaultPlanIfAbsent(
                "PRO_STUDENT_MONTHLY",
                "Gói Học Sinh Pro (Tháng)",
                PlanUserType.STUDENT,
                "Gói học sinh Pro tháng: Không giới hạn câu hỏi AI Tutor, làm đề không giới hạn, giảm 20% khi mua khóa học/bài học lẻ, tải PDF chuẩn.",
                new BigDecimal("99000.00"),
                BillingCycle.MONTHLY,
                -1, // Unlimited exams
                -1, // Unlimited AI questions
                null, null, null, null,
                "AI_TUTOR,PREMIUM_COURSES,PREMIUM_EXAMS,PDF_DOWNLOAD,VIDEO_HIGH_QUALITY,ADVANCED_ANALYTICS,DISCOUNT_ON_PURCHASES"
        );

        createDefaultPlanIfAbsent(
                "PRO_STUDENT_YEARLY",
                "Gói Học Sinh Pro (Năm)",
                PlanUserType.STUDENT,
                "Gói học sinh Pro năm: Tiết kiệm tối đa, toàn quyền sử dụng tất cả tính năng Pro suốt 12 tháng kèm ưu đãi giảm 20% mua nội dung.",
                new BigDecimal("799000.00"),
                BillingCycle.YEARLY,
                -1, // Unlimited exams
                -1, // Unlimited AI questions
                null, null, null, null,
                "AI_TUTOR,PREMIUM_COURSES,PREMIUM_EXAMS,PDF_DOWNLOAD,VIDEO_HIGH_QUALITY,ADVANCED_ANALYTICS,DISCOUNT_ON_PURCHASES"
        );

        createDefaultPlanIfAbsent(
                "ULTRA_STUDENT_MONTHLY",
                "Gói Học Sinh Ultra (Tháng)",
                PlanUserType.STUDENT,
                "Gói đặc quyền tối thượng Coursera Plus: Vào học MIỄN PHÍ TOÀN BỘ khóa học & bài học (tối đa 10 khóa/ngày, 100 khóa/tháng), AI Tutor không giới hạn tuyệt đối.",
                new BigDecimal("1500000.00"),
                BillingCycle.MONTHLY,
                -1, // Unlimited exams
                -1, // Unlimited AI questions
                null, null, 10, 100, // 10 courses/day, 100 courses/month
                "AI_TUTOR,PREMIUM_COURSES,PREMIUM_EXAMS,PDF_DOWNLOAD,VIDEO_HIGH_QUALITY,ADVANCED_ANALYTICS,DISCOUNT_ON_PURCHASES,ULTRA_UNLIMITED_COURSES,ULTRA_COURSE_ENROLLMENT"
        );

        // 2. Teacher Plans
        createDefaultPlanIfAbsent(
                "FREE_TEACHER",
                "Gói Giáo Viên Miễn Phí",
                PlanUserType.TEACHER,
                "Gói giáo viên cơ bản: Tối đa 5 lớp/khóa học và 100 câu hỏi trong ngân hàng câu hỏi.",
                BigDecimal.ZERO,
                BillingCycle.LIFETIME,
                null, null,
                5,  // 5 classes
                100, // 100 questions
                null, null,
                "PREMIUM_COURSES,PREMIUM_EXAMS"
        );

        createDefaultPlanIfAbsent(
                "TEACHER_PRO_MONTHLY",
                "Gói Giáo Viên Pro (Tháng)",
                PlanUserType.TEACHER,
                "Gói giáo viên chuyên nghiệp: Không giới hạn lớp & câu hỏi, AI tạo đề & câu hỏi thông minh, 0% phí sàn khi học sinh Pro mua nội dung.",
                new BigDecimal("149000.00"),
                BillingCycle.MONTHLY,
                null, null,
                -1, // Unlimited classes
                -1, // Unlimited questions
                null, null,
                "AI_TUTOR,PREMIUM_COURSES,PREMIUM_EXAMS,PDF_DOWNLOAD,VIDEO_HIGH_QUALITY,AI_EXAM_GENERATION,ADVANCED_ANALYTICS"
        );

        createDefaultPlanIfAbsent(
                "TEACHER_PRO_YEARLY",
                "Gói Giáo Viên Pro (Năm)",
                PlanUserType.TEACHER,
                "Gói giáo viên chuyên nghiệp năm: Tiết kiệm chi phí, không giới hạn lớp học và toàn quyền dùng bộ công cụ AI giáo dục.",
                new BigDecimal("1200000.00"),
                BillingCycle.YEARLY,
                null, null,
                -1, // Unlimited classes
                -1, // Unlimited questions
                null, null,
                "AI_TUTOR,PREMIUM_COURSES,PREMIUM_EXAMS,PDF_DOWNLOAD,VIDEO_HIGH_QUALITY,AI_EXAM_GENERATION,ADVANCED_ANALYTICS"
        );
    }

    private void createDefaultPlanIfAbsent(
            String code, String name, PlanUserType userType, String desc,
            BigDecimal price, BillingCycle cycle,
            Integer examLimitWeek, Integer aiQuestionLimitDay,
            Integer maxClasses, Integer maxQuestions,
            Integer dailyCourseEnrollmentLimit, Integer monthlyCourseEnrollmentLimit,
            String features
    ) {
        if (membershipPlanRepository.findByPlanCodeAndIsDeletedFalse(code).isEmpty()) {
            MembershipPlan plan = MembershipPlan.builder()
                    .planCode(code)
                    .name(name)
                    .userType(userType)
                    .description(desc)
                    .price(price)
                    .currency("VND")
                    .billingCycle(cycle)
                    .active(true)
                    .examLimitPerWeek(examLimitWeek)
                    .aiQuestionLimitPerDay(aiQuestionLimitDay)
                    .maxClassesLimit(maxClasses)
                    .maxQuestionsLimit(maxQuestions)
                    .dailyCourseEnrollmentLimit(dailyCourseEnrollmentLimit)
                    .monthlyCourseEnrollmentLimit(monthlyCourseEnrollmentLimit)
                    .features(features)
                    .status(ProductStatus.PUBLISHED)
                    .build();
            membershipPlanRepository.save(plan);
            log.info("Initialized default baseline plan: {} ({})", name, code);
        }
    }
}
