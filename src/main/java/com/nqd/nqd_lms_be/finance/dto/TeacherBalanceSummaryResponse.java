package com.nqd.nqd_lms_be.finance.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherBalanceSummaryResponse {
    private BigDecimal totalEarned;
    private BigDecimal availableBalance;
    private BigDecimal pendingBalance;
    private BigDecimal withdrawnAmount;
    private BigDecimal reversedAmount;
    private String currency;
}