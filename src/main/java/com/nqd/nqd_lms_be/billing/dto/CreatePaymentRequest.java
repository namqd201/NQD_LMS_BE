package com.nqd.nqd_lms_be.billing.dto;

import com.nqd.nqd_lms_be.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequest {

    @NotNull(message = "Mã đơn hàng (orderId) không được để trống")
    private UUID orderId;

    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.VIETQR;

    private String provider;

    private String returnUrl;

    private String cancelUrl;
}
