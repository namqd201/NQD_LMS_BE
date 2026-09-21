package com.nqd.nqd_lms_be.dto.exercise;

import com.nqd.nqd_lms_be.entity.enums.ExerciseAttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExerciseAttemptResultResponse {
    private UUID attemptId;
    private UUID exerciseId;
    private String exerciseTitle;
    private Integer attemptNumber;
    private ExerciseAttemptStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private BigDecimal totalScore;
    private BigDecimal maxScore;
    private BigDecimal percentage;
    private Boolean passed;
    private Integer correctCount;
    private Integer totalQuestions;
    private Boolean canRetry;
    private Integer remainingAttempts;
    private Long cooldownRemainingSeconds;
    private Integer attemptCycleCount;
    private Integer maxCycleAttempts;
    private List<StudentExerciseQuestionResultResponse> questionResults;
}
