package com.nqd.nqd_lms_be.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGradingResult {
    private boolean isCorrect;
    private BigDecimal marksAwarded;
    private String feedback;
}
