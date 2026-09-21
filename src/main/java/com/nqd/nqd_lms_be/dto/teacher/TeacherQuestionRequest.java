package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionStatus;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
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
public class TeacherQuestionRequest {
    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    private UUID categoryId;
    private UUID courseId;
    private UUID lessonId;
    private String gradeLevel;

    @NotNull(message = "Question type is required")
    private QuestionType questionType;

    @NotNull(message = "Difficulty is required")
    private QuestionDifficulty difficulty;

    @NotBlank(message = "Content is required")
    private String content;

    private String explanation;
    private BigDecimal defaultMarks;
    private QuestionStatus status;
    private List<String> tags;
    private List<TeacherQuestionOptionDto> options;
}
