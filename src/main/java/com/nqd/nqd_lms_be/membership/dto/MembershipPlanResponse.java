package com.nqd.nqd_lms_be.membership.dto;

import com.nqd.nqd_lms_be.entity.MembershipPlan;
import com.nqd.nqd_lms_be.entity.enums.BillingCycle;
import com.nqd.nqd_lms_be.entity.enums.PlanUserType;
import com.nqd.nqd_lms_be.entity.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlanResponse {
    private UUID id;
    private String planCode;
    private String name;
    private PlanUserType userType;
    private String description;
    private BigDecimal price;
    private String currency;
    private BillingCycle billingCycle;
    private Boolean active;
    private Integer examLimitPerWeek;
    private Integer aiQuestionLimitPerDay;
    private Integer maxClassesLimit;
    private Integer maxQuestionsLimit;
    private Integer aiPromptLimitPerMonth;
    private Integer examCreationLimit;
    private Integer dailyCourseEnrollmentLimit;
    private Integer monthlyCourseEnrollmentLimit;
    private Set<String> features;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MembershipPlanResponse fromEntity(MembershipPlan plan) {
        if (plan == null) return null;
        return MembershipPlanResponse.builder()
                .id(plan.getId())
                .planCode(plan.getPlanCode())
                .name(plan.getName())
                .userType(plan.getUserType())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .currency(plan.getCurrency())
                .billingCycle(plan.getBillingCycle())
                .active(plan.getActive())
                .examLimitPerWeek(plan.getExamLimitPerWeek())
                .aiQuestionLimitPerDay(plan.getAiQuestionLimitPerDay())
                .maxClassesLimit(plan.getMaxClassesLimit())
                .maxQuestionsLimit(plan.getMaxQuestionsLimit())
                .aiPromptLimitPerMonth(plan.getAiPromptLimitPerMonth())
                .examCreationLimit(plan.getExamCreationLimit())
                .dailyCourseEnrollmentLimit(plan.getDailyCourseEnrollmentLimit())
                .monthlyCourseEnrollmentLimit(plan.getMonthlyCourseEnrollmentLimit())
                .features(plan.getFeatureSet())
                .status(plan.getStatus())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
