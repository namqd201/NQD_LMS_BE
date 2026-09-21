package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.AiJobStatus;
import com.nqd.nqd_lms_be.entity.enums.AiJobType;
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
public class TeacherAiJobResponse {
    private UUID id;
    private AiJobType jobType;
    private AiJobStatus status;
    private String promptSummary;
    private UUID subjectId;
    private String subjectName;
    private UUID courseId;
    private String courseTitle;
    private UUID lessonId;
    private String lessonTitle;
    private String gradeLevel;
    private String topic;
    private Integer totalRequested;
    private Integer totalGenerated;
    private Integer totalApproved;
    private String targetExamTitle;
    private String targetExamCode;
    private Integer targetExamDuration;
    private BigDecimal targetExamPassingMarks;
    private BigDecimal targetExamTotalMarks;
    private UUID createdExamId;
    private UUID categoryId;
    private String categoryName;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
