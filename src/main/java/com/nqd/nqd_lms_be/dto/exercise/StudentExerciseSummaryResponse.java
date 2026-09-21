package com.nqd.nqd_lms_be.dto.exercise;

import com.nqd.nqd_lms_be.entity.enums.ExerciseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExerciseSummaryResponse {
    private UUID id;
    private UUID lessonId;
    private String lessonTitle;
    private String title;
    private String description;
    private String instructions;
    private ExerciseType type;
    private Integer timeLimitMinutes;
    private BigDecimal passingScore;
    private Integer questionCount;
    private BigDecimal totalMarks;
    private Integer maxAttempts;
    private Boolean showExplanationImmediately;
    private Boolean allowRetry;
    private Long userAttemptsCount;
    private BigDecimal userBestScore;
    private BigDecimal userBestPercentage;
    private Boolean userPassed;
    private Boolean hasInProgressAttempt;
    private UUID inProgressAttemptId;
    private Long cooldownRemainingSeconds;
    private Integer attemptCycleCount;
    private Integer maxCycleAttempts;
}
