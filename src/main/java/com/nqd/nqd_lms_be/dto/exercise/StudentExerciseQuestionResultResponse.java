package com.nqd.nqd_lms_be.dto.exercise;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExerciseQuestionResultResponse {
    private UUID questionId;
    private UUID selectedOptionId;
    private String selectedOptionKey;
    private String answerText;
    private Boolean isCorrect;
    private UUID correctOptionId;
    private String correctOptionKey;
    private String explanation;
    private BigDecimal marksAwarded;
    private BigDecimal maxMarks;
    private String aiExplanation;
    private LocalDateTime answeredAt;
}
