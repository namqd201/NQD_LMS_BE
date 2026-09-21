package com.nqd.nqd_lms_be.dto.exercise;

import com.nqd.nqd_lms_be.entity.enums.ExerciseStatus;
import com.nqd.nqd_lms_be.entity.enums.ExerciseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherExerciseRequest {
    @NotNull(message = "Bài học không được để trống")
    private UUID lessonId;

    @NotBlank(message = "Tiêu đề bài tập không được để trống")
    private String title;

    private String description;
    private String instructions;

    @Builder.Default
    private ExerciseType type = ExerciseType.PRACTICE;

    private Integer timeLimitMinutes;
    private BigDecimal passingScore;

    @Builder.Default
    private ExerciseStatus status = ExerciseStatus.DRAFT;

    private Integer maxAttempts;

    @Builder.Default
    private Boolean showExplanationImmediately = true;

    @Builder.Default
    private Boolean allowRetry = true;

    private List<TeacherExerciseQuestionItemRequest> questions;
}
