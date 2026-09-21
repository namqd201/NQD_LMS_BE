package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus;
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
public class TeacherStudentLessonProgressResponse {
    private UUID progressId;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private UUID lessonId;
    private String lessonTitle;
    private UUID courseId;
    private LessonProgressStatus status;
    private BigDecimal progressPercent;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;
    private Boolean isUnlockedByAdmin;
}
