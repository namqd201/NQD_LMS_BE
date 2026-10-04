package com.nqd.nqd_lms_be.dto.gamification;

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
public class GamificationDashboardResponse {
    private UUID studentId;
    private String studentName;
    private String avatarUrl;

    // Metric 1: Thời gian học tuần này
    private Double studyHoursThisWeek;
    private Double studyTargetHours;
    private Double studyGrowthPercent;
    private List<Double> dailyStudyHoursOfWeek; // T2, T3, T4, T5, T6, T7, CN

    // Metric 2: Nhiệm vụ hôm nay
    private Integer dailyMissionsCompleted;
    private Integer dailyMissionsTotal;
    private Integer dailyCompletionPercent;
    private List<DailyMissionDto> dailyMissions;
    private Boolean dailyRewardClaimed;
    private String nextMissionTitle;

    // Metric 3: Chuỗi ngày học liên tục (Streak)
    private Integer currentStreak;
    private Integer longestStreak;
    private Boolean hasStudiedToday;
    private List<Boolean> weeklyStreakStatus; // 7 ngày trong tuần: true/false

    // Metric 4: Điểm năng động XP
    private Long totalXp;
    private Long monthlyXp;
    private Long todayXp;
    private Long monthlyRank;
    private String rankTier;
    private String rankTierName;
}
