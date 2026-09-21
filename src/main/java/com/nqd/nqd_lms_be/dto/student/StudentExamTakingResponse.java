package com.nqd.nqd_lms_be.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExamTakingResponse {
    private UUID examId;
    private UUID attemptId;
    private String title;
    private String description;
    private String instructions;
    private Integer durationMinutes;
    private BigDecimal totalMarks;
    private BigDecimal passingMarks;
    private Integer attemptNumber;
    private LocalDateTime startedAt;
    private Boolean enableProctoring;
    private Integer maxViolationCount;
    private Integer violationCount;
    private Boolean isFlagged;
    private List<StudentQuestionTakingResponse> questions;
}
