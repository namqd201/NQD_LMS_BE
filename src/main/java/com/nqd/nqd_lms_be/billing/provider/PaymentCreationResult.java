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
public class PaymentCreationResult {

    private boolean success;
    private String provider;
    private String providerTransactionId;
    private String orderCode;
    private BigDecimal amount;
    private String currency;
    private String qrCode;
    private String checkoutUrl;
    private String signature;
    private LocalDateTime expiresAt;
    private String errorMessage;
}
