package com.nqd.nqd_lms_be.dto.student;

import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Question DTO presented to students during an exam attempt.
 * Strictly DOES NOT contain `explanation` or answer keys.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentQuestionTakingResponse {
    private UUID questionId;
    private String content;
    private QuestionType questionType;
    private QuestionDifficulty difficulty;
    private BigDecimal marks;
    private Integer displayOrder;
    private List<StudentOptionTakingResponse> options;
}
