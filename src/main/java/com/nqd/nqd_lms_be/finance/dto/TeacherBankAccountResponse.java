package com.nqd.nqd_lms_be.finance.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherBankAccountResponse {
    private UUID id;
    private String bankName;
    private String bankCode;
    private String accountNumberMasked;
    private String accountHolderName;
    private Boolean isDefault;
    private Boolean isVerified;
}