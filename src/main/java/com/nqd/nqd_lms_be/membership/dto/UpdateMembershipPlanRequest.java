package com.nqd.nqd_lms_be.membership.dto;

import com.nqd.nqd_lms_be.entity.enums.BillingCycle;
import com.nqd.nqd_lms_be.entity.enums.PlanUserType;
import com.nqd.nqd_lms_be.entity.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMembershipPlanRequest {

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

    private Set<String> features;
    private ProductStatus status;
}
