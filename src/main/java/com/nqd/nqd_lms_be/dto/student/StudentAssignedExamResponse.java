package com.nqd.nqd_lms_be.dto.student;

import com.nqd.nqd_lms_be.entity.enums.ExamStatus;
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
public class StudentAssignedExamResponse {
    private UUID examId;
    private String code;
    private String title;
    private String description;
    private String instructions;
    private UUID subjectId;
    private String subjectName;
    private String gradeLevel;
    private UUID courseId;
    private String courseName;
    private Integer durationMinutes;
    private BigDecimal totalMarks;
    private BigDecimal passingMarks;
    private Integer maxAttempts;
    private Integer questionCount;
    private ExamStatus status;
    private Integer attemptsTaken;
    private BigDecimal bestScore;
    private Boolean passed;
    private LocalDateTime assignedAt;
    private String teacherName;
}
