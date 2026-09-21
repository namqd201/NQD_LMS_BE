package com.nqd.nqd_lms_be.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminGrantVipRequest {
    private UUID planId;
    private String planCode;
    private Integer durationMonths; // 1, 3, 6, 12, or -1 for Lifetime
    private String reason; // e.g. "Người thân admin", "Tester dự án"
}
