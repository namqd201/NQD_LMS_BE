package com.nqd.nqd_lms_be.dto.exercise;

import com.nqd.nqd_lms_be.entity.enums.ExerciseStatus;
import com.nqd.nqd_lms_be.entity.enums.ExerciseType;
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
public class TeacherExerciseResponse {
    private UUID id;
    private UUID lessonId;
    private String lessonTitle;
    private UUID chapterId;
    private String chapterTitle;
    private UUID courseId;
    private String courseTitle;
    private UUID subjectId;
    private String subjectName;
    private String title;
    private String description;
    private String instructions;
    private ExerciseType type;
    private Integer timeLimitMinutes;
    private BigDecimal passingScore;
    private ExerciseStatus status;
    private Integer maxAttempts;
    private Boolean showExplanationImmediately;
    private Boolean allowRetry;
    private Integer questionCount;
    private BigDecimal totalMarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String creatorName;
    private List<TeacherExerciseQuestionResponse> questions;
}
