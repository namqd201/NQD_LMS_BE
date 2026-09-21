package com.nqd.nqd_lms_be.billing.dto;

import com.nqd.nqd_lms_be.entity.enums.PaymentStatus;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFilterRequest {

    private PaymentStatus status;
    private String provider;
    private String orderCode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime toDate;
}
