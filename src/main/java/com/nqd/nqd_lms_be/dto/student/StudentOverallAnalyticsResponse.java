package com.nqd.nqd_lms_be.dto.student;

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
public class StudentOverallAnalyticsResponse {
    private UUID studentId;
    private String studentName;
    private String studentEmail;

    // Core KPIs
    private Double totalStudyHours;
    private long totalCompletedLessons;
    private long totalEnrolledCourses;
    private long totalCompletedCourses;
    private Double averageExamScore;
    private long totalExamsTaken;
    private long totalExamsPassed;

    // Streaks
    private int currentStreakDays;
    private int longestStreakDays;

    // Activity Heatmap (Daily interaction count for the past 365 days)
    private List<DailyHeatmapItemDto> activityHeatmap;

    // Recent Course Progress Summary
    private List<StudentCourseProgressSummaryDto> recentCourses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyHeatmapItemDto {
        private String date; // YYYY-MM-DD
        private int count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentCourseProgressSummaryDto {
        private UUID courseId;
        private String courseTitle;
        private String courseThumbnail;
        private BigDecimal progressPercent;
        private long completedLessons;
        private long totalLessons;
        private String enrollmentStatus;
    }
}
