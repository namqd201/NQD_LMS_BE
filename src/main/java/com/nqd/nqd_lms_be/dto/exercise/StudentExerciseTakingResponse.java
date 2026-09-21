package com.nqd.nqd_lms_be.dto.exercise;

import com.nqd.nqd_lms_be.entity.enums.ExerciseType;
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
public class StudentExerciseTakingResponse {
    private UUID exerciseId;
    private UUID attemptId;
    private Integer attemptNumber;
    private String title;
    private String description;
    private String instructions;
    private ExerciseType type;
    private Integer timeLimitMinutes;
    private BigDecimal passingScore;
    private Boolean showExplanationImmediately;
    private Boolean allowRetry;
    private LocalDateTime startedAt;
    private Integer totalQuestions;
    private BigDecimal totalMarks;
    private List<StudentExerciseQuestionTakingResponse> questions;
    private List<StudentExerciseQuestionResultResponse> answeredQuestions;
    private Long cooldownRemainingSeconds;
    private Integer attemptCycleCount;
    private Integer maxCycleAttempts;
}
