package com.nqd.nqd_lms_be.dto.teacher;

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
public class TeacherExamResultsSummaryResponse {
    private UUID examId;
    private String examCode;
    private String title;
    private String subjectName;
    private String gradeLevel;
    private Integer durationMinutes;
    private BigDecimal totalMarks;
    private BigDecimal passingMarks;
    private Integer maxAttempts;
    private Integer questionCount;
    private Long totalAssigned;
    private Long totalSubmissions;
    private Long passedCount;
    private BigDecimal averageScore;
    private BigDecimal highestScore;
    private List<TeacherExamStudentAttemptResponse> attempts;
}
