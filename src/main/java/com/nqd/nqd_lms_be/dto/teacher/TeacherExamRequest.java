package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.ExamStatus;
import com.nqd.nqd_lms_be.entity.enums.ExamVisibility;
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
public class TeacherExamRequest {
    private UUID courseId;

    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    private String code;
    private String gradeLevel;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String instructions;

    @NotNull(message = "Duration in minutes is required")
    private Integer durationMinutes;

    private BigDecimal totalMarks;
    private BigDecimal passingMarks;
    private Integer maxAttempts;
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;
    private ExamStatus status;
    private ExamVisibility visibility;
    private Boolean enableProctoring;
    private Integer maxViolationCount;
    private List<UUID> questionIds;
}
