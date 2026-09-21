package com.nqd.nqd_lms_be.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessRefundRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    private String refundReason;
}