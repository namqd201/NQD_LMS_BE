package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.ai.dto.GeneratedOptionDraft;
import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAiUpdateGeneratedQuestionRequest {

    @NotBlank(message = "Question content cannot be empty")
    private String content;

    @NotNull(message = "Question type is required")
    private QuestionType questionType;

    @NotNull(message = "Difficulty is required")
    private QuestionDifficulty difficulty;

    @NotNull(message = "Marks is required")
    private BigDecimal marks;

    private String explanation;

    private String tags;

    private String audioUrl;

    private String audioScript;

    @Builder.Default
    private List<GeneratedOptionDraft> options = new ArrayList<>();
}
