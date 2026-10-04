package com.nqd.nqd_lms_be.service.gamification;

import com.nqd.nqd_lms_be.dto.gamification.ClaimMissionResponse;
import com.nqd.nqd_lms_be.dto.gamification.GamificationDashboardResponse;
import com.nqd.nqd_lms_be.dto.gamification.LeaderboardResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface GamificationService {

    GamificationDashboardResponse getStudentDashboard(UUID studentId);

    LeaderboardResponse getMonthlyLeaderboard(UUID currentUserId, String monthYear);

    LeaderboardResponse getAllTimeLeaderboard(UUID currentUserId);

    ClaimMissionResponse claimDailyMission(UUID studentId, UUID missionId);

    void onLessonCompleted(UUID studentId, UUID lessonId);

    void onExerciseCompleted(UUID studentId, UUID exerciseId, boolean passed, BigDecimal scorePercentage);

    void onExamSubmitted(UUID studentId, UUID examId, boolean passed, BigDecimal scorePercentage);

    void processDailyStreakResets();

    void processMonthlyRewards(String targetMonthYear);
}
