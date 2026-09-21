package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamBlueprintItemRequest {

    private String topic;

    private QuestionType questionType;

    private QuestionDifficulty difficulty;

    @Min(value = 1, message = "Count must be at least 1")
    @Builder.Default
    private Integer count = 1;

    @Builder.Default
    private BigDecimal marksPerQuestion = new BigDecimal("1.00");
}
