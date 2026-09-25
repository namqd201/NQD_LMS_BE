package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionStatus;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
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
public class TeacherQuestionResponse {
    private UUID id;
    private UUID subjectId;
    private String subjectName;
    private UUID categoryId;
    private String categoryName;
    private UUID courseId;
    private String courseName;
    private UUID lessonId;
    private String lessonTitle;
    private String gradeLevel;
    private QuestionType questionType;
    private QuestionDifficulty difficulty;
    private String content;
    private String audioUrl;
    private String audioScript;
    private String explanation;
    private BigDecimal defaultMarks;
    private QuestionStatus status;
    private UUID creatorId;
    private String creatorName;
    private List<String> tags;
    private List<TeacherQuestionOptionDto> options;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private String deletedBy;
}
