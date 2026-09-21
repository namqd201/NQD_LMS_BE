package com.nqd.nqd_lms_be.finance.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPlatformRevenueResponse {
    private BigDecimal grossRevenue;
    private BigDecimal platformTotalFees;
    private BigDecimal teacherTotalEarnings;
    private BigDecimal completedWithdrawals;
    private BigDecimal pendingWithdrawals;
    private BigDecimal reversedAmount;
    private String currency;
}