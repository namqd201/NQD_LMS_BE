package com.nqd.nqd_lms_be.dto.exercise;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExerciseAiExplainResponse {
    private UUID questionId;
    private String questionContent;
    private String studentAnswer;
    private String correctAnswer;
    private String explanation;
}
