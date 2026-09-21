package com.nqd.nqd_lms_be.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAttemptEventBatchResponse {
    private UUID attemptId;
    private Integer violationCount;
    private Integer maxViolationCount;
    private Boolean isFlagged;
    private String flagReason;
    private Boolean isAutoSubmitted;
    private String message;
}
