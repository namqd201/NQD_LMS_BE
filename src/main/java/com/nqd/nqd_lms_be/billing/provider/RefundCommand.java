package com.nqd.nqd_lms_be.billing.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundCommand {

    private String providerTransactionId;
    private String orderCode;
    private BigDecimal refundAmount;
    private String reason;
}
