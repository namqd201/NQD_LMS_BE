package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class TeacherAiGenerateQuestionsRequest {

    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    private UUID courseId;

    private UUID categoryId;

    private UUID lessonId;

    private String gradeLevel;

    @NotBlank(message = "Topic is required")
    private String topic;

    private QuestionType questionType;

    private List<QuestionType> questionTypes;

    private QuestionDifficulty difficulty;

    @Min(value = 1, message = "Must request at least 1 question")
    @Max(value = 50, message = "Maximum 50 questions per generation batch")
    @Builder.Default
    private Integer numberOfQuestions = 5;

    @Builder.Default
    private BigDecimal marksPerQuestion = new BigDecimal("1.00");

    private String additionalInstructions;
    @Builder.Default
    private Boolean isListening = false;
    private String listeningPassageType;
}
