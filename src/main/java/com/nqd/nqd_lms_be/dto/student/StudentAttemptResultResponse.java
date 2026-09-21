package com.nqd.nqd_lms_be.dto.student;

import com.nqd.nqd_lms_be.entity.enums.ExamAttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttemptResultResponse {
    private UUID attemptId;
    private UUID examId;
    private String examTitle;
    private Integer attemptNumber;
    private ExamAttemptStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private BigDecimal totalScore;
    private BigDecimal maxScore;
    private BigDecimal percentage;
    private Boolean passed;
    private Integer violationCount;
    private Boolean isFlagged;
    private String flagReason;
}
