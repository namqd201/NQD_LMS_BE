package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.BillingCycle;
import com.nqd.nqd_lms_be.entity.enums.FeatureKey;
import com.nqd.nqd_lms_be.entity.enums.PlanUserType;
import com.nqd.nqd_lms_be.entity.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(
    name = "membership_plans",
    indexes = {
        @Index(name = "idx_membership_plan_code", columnList = "plan_code", unique = true),
        @Index(name = "idx_membership_plan_user_type", columnList = "user_type"),
        @Index(name = "idx_membership_plan_status", columnList = "status, active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class MembershipPlan extends BaseEntity {

    @Column(name = "plan_code", nullable = false, unique = true, length = 64)
    private String planCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 32)
    @Builder.Default
    private PlanUserType userType = PlanUserType.STUDENT;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    // Structured Limit Configurations (null or -1 means UNLIMITED)
    @Column(name = "exam_limit_per_week")
    private Integer examLimitPerWeek;

    @Column(name = "ai_question_limit_per_day")
    private Integer aiQuestionLimitPerDay;

    @Column(name = "max_classes_limit")
    private Integer maxClassesLimit;

    @Column(name = "max_questions_limit")
    private Integer maxQuestionsLimit;

    @Column(name = "ai_prompt_limit_per_month")
    @Builder.Default
    private Integer aiPromptLimitPerMonth = 100;

    @Column(name = "exam_creation_limit")
    @Builder.Default
    private Integer examCreationLimit = 50;

    @Column(name = "daily_course_enrollment_limit")
    private Integer dailyCourseEnrollmentLimit;

    @Column(name = "monthly_course_enrollment_limit")
    private Integer monthlyCourseEnrollmentLimit;

    @Column(name = "features", columnDefinition = "TEXT")
    private String features;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.PUBLISHED;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    public boolean isFree() {
        return price == null || price.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isStudentPlan() {
        return userType == PlanUserType.STUDENT || userType == PlanUserType.ALL;
    }

    public boolean isTeacherPlan() {
        return userType == PlanUserType.TEACHER || userType == PlanUserType.ALL;
    }

    public Set<String> getFeatureSet() {
        if (features == null || features.isBlank()) {
            return new HashSet<>();
        }
        return Arrays.stream(features.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public boolean hasFeature(FeatureKey featureKey) {
        if (featureKey == null) return false;
        return getFeatureSet().contains(featureKey.name());
    }
}
