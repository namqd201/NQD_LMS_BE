package com.nqd.nqd_lms_be.ai.dto;

import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamBlueprintItem {
    private String topic;
    private QuestionType questionType;
    private QuestionDifficulty difficulty;
    @Builder.Default
    private Integer count = 1;
    @Builder.Default
    private BigDecimal marksPerQuestion = new BigDecimal("1.00");
}
