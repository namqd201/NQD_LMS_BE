package com.nqd.nqd_lms_be.dto.teacher;

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
public class TeacherExamStudentAttemptResponse {
    private UUID attemptId;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private String studentAvatar;
    private Integer attemptNumber;
    private ExamAttemptStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Long durationSeconds;
    private BigDecimal totalScore;
    private BigDecimal maxScore;
    private BigDecimal percentage;
    private Boolean passed;
    private Long correctAnswersCount;
    private Long totalQuestionsCount;
}
