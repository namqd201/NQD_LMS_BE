package com.nqd.nqd_lms_be.billing.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreationCommand {

    private String orderCode;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String buyerEmail;
    private String buyerName;
    private String buyerPhone;
    private String returnUrl;
    private String cancelUrl;
    private LocalDateTime expiresAt;
    private String idempotencyKey;
}
