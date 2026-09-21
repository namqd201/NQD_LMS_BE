package com.nqd.nqd_lms_be.dto.exercise;

import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
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
public class TeacherExerciseQuestionResponse {
    private UUID questionId;
    private Integer displayOrder;
    private BigDecimal marks;
    private String content;
    private QuestionType questionType;
    private QuestionDifficulty difficulty;
    private String explanation;
    private List<TeacherExerciseOptionResponse> options;
}
