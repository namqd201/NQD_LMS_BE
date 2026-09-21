package com.nqd.nqd_lms_be.billing.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResult {

    private boolean success;
    private String refundTransactionId;
    private String message;
}
