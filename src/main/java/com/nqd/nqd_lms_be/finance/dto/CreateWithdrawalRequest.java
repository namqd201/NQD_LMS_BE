package com.nqd.nqd_lms_be.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWithdrawalRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "50000.00", message = "Minimum withdrawal amount is 50,000 VND")
    private BigDecimal amount;

    private UUID bankAccountId;

    private String bankName;
    private String bankCode;
    private String accountNumber;
    private String accountHolderName;

    private String idempotencyKey;
}