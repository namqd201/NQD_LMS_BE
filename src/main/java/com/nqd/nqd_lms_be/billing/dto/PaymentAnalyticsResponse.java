package com.nqd.nqd_lms_be.billing.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAnalyticsResponse {

    private BigDecimal revenueToday;
    private BigDecimal revenueThisMonth;
    private BigDecimal totalRevenue;
    private long paidOrdersCount;
    private long pendingOrdersCount;
    private long failedPaymentsCount;
    private long successfulPaymentsCount;
}
