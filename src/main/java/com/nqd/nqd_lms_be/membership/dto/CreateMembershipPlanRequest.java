package com.nqd.nqd_lms_be.membership.dto;

import com.nqd.nqd_lms_be.entity.enums.BillingCycle;
import com.nqd.nqd_lms_be.entity.enums.PlanUserType;
import com.nqd.nqd_lms_be.entity.enums.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateMembershipPlanRequest {

    @NotBlank(message = "Mã gói dịch vụ không được để trống")
    private String planCode;

    @NotBlank(message = "Tên gói dịch vụ không được để trống")
    private String name;

    @NotNull(message = "Loại người dùng không được để trống")
    private PlanUserType userType;

    private String description;

    @NotNull(message = "Giá tiền không được để trống")
    private BigDecimal price;

    @Builder.Default
    private String currency = "VND";

    @NotNull(message = "Chu kỳ thanh toán không được để trống")
    private BillingCycle billingCycle;

    @Builder.Default
    private Boolean active = true;

    private Integer examLimitPerWeek;
    private Integer aiQuestionLimitPerDay;
    private Integer maxClassesLimit;
    private Integer maxQuestionsLimit;
    private Integer aiPromptLimitPerMonth;
    private Integer examCreationLimit;

    private Set<String> features;
    private ProductStatus status;
}
