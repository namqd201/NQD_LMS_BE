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
public class TeacherEarningResponse {
    private UUID id;
    private UUID orderId;
    private String orderCode;
    private UUID orderItemId;
    private UUID courseId;
    private String courseName;
    private BigDecimal grossAmount;
    private BigDecimal platformFeeRate;
    private BigDecimal platformFee;
    private BigDecimal teacherShareRate;
    private BigDecimal teacherAmount;
    private String currency;
    private String status;
    private LocalDateTime availableAt;
    private LocalDateTime reversedAt;
    private String reversalReason;
    private LocalDateTime createdAt;
}