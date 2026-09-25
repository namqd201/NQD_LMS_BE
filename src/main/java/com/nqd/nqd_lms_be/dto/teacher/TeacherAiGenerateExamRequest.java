package com.nqd.nqd_lms_be.dto.teacher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAiGenerateExamRequest {

    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    private UUID courseId;

    @NotBlank(message = "Exam Title is required")
    private String title;

    private String gradeLevel;

    private String topic;

    @NotNull(message = "Duration minutes is required")
    private Integer durationMinutes;

    @NotNull(message = "Passing marks is required")
    private BigDecimal passingMarks;

    private BigDecimal totalMarks;

    @Builder.Default
    private List<ExamBlueprintItemRequest> blueprintItems = new ArrayList<>();

    private String additionalInstructions;
    @Builder.Default
    private Boolean isListening = false;
    private String listeningPassageType;
    @Builder.Default
    private Integer maxListeningPlays = 2;
}
