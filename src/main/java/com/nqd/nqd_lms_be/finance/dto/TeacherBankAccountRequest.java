package com.nqd.nqd_lms_be.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherBankAccountRequest {

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Bank code is required")
    private String bankCode;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;

    private Boolean isDefault;
}