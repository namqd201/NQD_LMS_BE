package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.ExamStatus;
import com.nqd.nqd_lms_be.entity.enums.ExamVisibility;
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
public class TeacherExamResponse {
    private UUID id;
    private String code;
    private String gradeLevel;
    private UUID subjectId;
    private String subjectName;
    private UUID courseId;
    private String courseName;
    private String title;
    private String description;
    private String instructions;
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
    private UUID originExamId;
    private String originExamTitle;
    private UUID creatorId;
    private String creatorName;
    private int questionCount;
    private List<TeacherQuestionResponse> questions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private String deletedBy;
}
