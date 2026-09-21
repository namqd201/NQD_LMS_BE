package com.nqd.nqd_lms_be.dto.exercise;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExerciseSubmitQuestionRequest {
    @NotNull(message = "Question ID không được để trống")
    private UUID questionId;

    private UUID selectedOptionId;
    private String answerText;
}
