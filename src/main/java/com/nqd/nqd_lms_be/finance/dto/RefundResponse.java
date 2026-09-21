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
public class RefundResponse {
    private UUID orderId;
    private String orderCode;
    private BigDecimal refundAmount;
    private String status;
    private String reason;
    private LocalDateTime refundedAt;
    private int reversedEarningsCount;
    private int revokedEntitlementsCount;
}