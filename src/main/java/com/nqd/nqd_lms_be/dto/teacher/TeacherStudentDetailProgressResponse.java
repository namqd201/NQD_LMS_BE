package com.nqd.nqd_lms_be.dto.teacher;

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
public class TeacherStudentDetailProgressResponse {
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private String avatarUrl;
    private String phone;
    
    // Overall Stats
    private int enrolledCoursesCount;
    private int completedCoursesCount;
    private long totalCompletedLessons;
    private Double overallAverageExamScore;
    private LocalDateTime lastActiveAt;

    // Enrolled Courses Detail
    private List<StudentCourseDetailDto> courses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentCourseDetailDto {
        private UUID courseId;
        private String courseName;
        private String courseThumbnail;
        private String enrollmentStatus;
        private LocalDateTime enrolledAt;
        private LocalDateTime completedAt;
        private BigDecimal progressPercent;
        private long completedLessonsCount;
        private long totalLessonsCount;
        private Double averageExamScore;
        private List<StudentLessonItemDto> lessonProgresses;
        private List<StudentExamAttemptDto> examAttempts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentLessonItemDto {
        private UUID lessonId;
        private String lessonTitle;
        private String chapterTitle;
        private String status;
        private BigDecimal progressPercent;
        private LocalDateTime lastAccessedAt;
        private LocalDateTime completedAt;
        private Boolean isUnlockedByAdmin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentExamAttemptDto {
        private UUID attemptId;
        private UUID examId;
        private String examTitle;
        private Integer attemptNumber;
        private BigDecimal totalScore;
        private BigDecimal percentage;
        private Boolean passed;
        private LocalDateTime submittedAt;
    }
}
