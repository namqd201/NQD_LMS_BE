package com.nqd.nqd_lms_be.finance.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProcessWithdrawalRequest {
    private String referenceCode;
    private String rejectionReason;
}