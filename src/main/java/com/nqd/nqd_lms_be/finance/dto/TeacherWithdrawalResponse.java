package com.nqd.nqd_lms_be.finance.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherWithdrawalResponse {
    private UUID id;
    private String withdrawalCode;
    private BigDecimal amount;
    private String currency;
    private String bankName;
    private String bankCode;
    private String accountNumberMasked;
    private String accountHolderName;
    private String status;
    private String referenceCode;
    private String rejectionReason;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private String processedBy;
}