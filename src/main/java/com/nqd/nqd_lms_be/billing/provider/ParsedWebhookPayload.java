package com.nqd.nqd_lms_be.billing.provider;

import com.nqd.nqd_lms_be.entity.enums.PaymentStatus;
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
public class ParsedWebhookPayload {

    private String provider;
    private String orderCode;
    private String providerTransactionId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private LocalDateTime transactionDateTime;
    private String gatewayReference;
    private String rawReference;
    private String rawPayload;
    private boolean isSignatureValid;
    private String errorMessage;
}
