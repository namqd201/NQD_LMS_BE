package com.nqd.nqd_lms_be.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAnalyticsResponse {
    private UUID courseId;
    private String courseName;

    // Enrollment Stats
    private long totalEnrolledStudents;
    private long activeStudents;
    private long completedStudents;
    private long droppedStudents;
    private Double completionRate;

    // Course Progress Stats
    private Double averageProgressPercent;
    private ProgressDistributionDto progressDistribution;

    // Exam & Assessment Stats
    private long totalExams;
    private long totalAttempts;
    private Double averageScore;
    private Double passRate;
    private long passedAttempts;
    private long failedAttempts;
    private ScoreDistributionDto scoreDistribution;

    // Drop-off Lessons (Bài học điểm rơi - nhiều học viên dừng lại nhất)
    private List<LessonDropOffDto> dropOffLessons;

    // Activity Timeline (Xu hướng học tập theo ngày)
    private List<DailyActivityDto> activityTimeline;

    // Top Students
    private List<TopStudentSummaryDto> topStudents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressDistributionDto {
        private long range0To25;
        private long range25To50;
        private long range50To75;
        private long range75To100;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreDistributionDto {
        private long below5;
        private long range5To7;
        private long range7To85;
        private long range85To10;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonDropOffDto {
        private UUID lessonId;
        private String title;
        private String chapterTitle;
        private Integer displayOrder;
        private long inProgressStudentsCount;
        private long completedStudentsCount;
        private Double dropOffRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyActivityDto {
        private String date; // YYYY-MM-DD
        private long completedLessons;
        private long examSubmissions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopStudentSummaryDto {
        private UUID studentId;
        private String studentName;
        private String studentEmail;
        private String avatarUrl;
        private Double progressPercent;
        private Double averageExamScore;
        private String enrollmentStatus;
        private String lastAccessedAt;
    }
}
