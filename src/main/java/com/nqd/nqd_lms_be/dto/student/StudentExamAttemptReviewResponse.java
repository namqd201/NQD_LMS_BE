package com.nqd.nqd_lms_be.dto.student;

import com.nqd.nqd_lms_be.entity.enums.ExamAttemptStatus;
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
public class StudentExamAttemptReviewResponse {
    private UUID attemptId;
    private UUID examId;
    private String examTitle;
    private String examCode;
    private String audioUrl;
    private String audioScript;
    private Integer attemptNumber;
    private ExamAttemptStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Long durationSeconds;
    private BigDecimal totalScore;
    private BigDecimal maxScore;
    private BigDecimal percentage;
    private Boolean passed;
    private List<StudentExamAnswerReviewResponse> answers;
}
