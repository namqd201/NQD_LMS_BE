package com.nqd.nqd_lms_be.membership.dto;

import com.nqd.nqd_lms_be.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscribePlanRequest {

    @NotNull(message = "ID gói dịch vụ không được để trống")
    private UUID planId;

    private String couponCode;

    private PaymentMethod paymentMethod;

    private String returnUrl;

    private String cancelUrl;

    private String idempotencyKey;
}
