package com.nqd.nqd_lms_be.service.gamification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class GamificationScheduler {

    private final GamificationService gamificationService;

    /**
     * Daily at 00:05 AM (VN +7): Check and reset streaks for users who didn't study yesterday.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void scheduleDailyStreakReset() {
        log.info("Starting daily streak verification job...");
        try {
            gamificationService.processDailyStreakResets();
            log.info("Daily streak verification completed successfully.");
        } catch (Exception e) {
            log.error("Error during daily streak verification job", e);
        }
    }

    /**
     * Every night at 23:59 PM (VN +7): Check if today is the last day of the month.
     * If so, finalize monthly leaderboard rewards and reset monthly XP.
     */
    @Scheduled(cron = "0 59 23 28-31 * *", zone = "Asia/Ho_Chi_Minh")
    public void scheduleMonthlyRewardFinalization() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // If tomorrow is day 1 of next month, today is the last day!
        if (tomorrow.getDayOfMonth() == 1) {
            String monthYear = today.format(DateTimeFormatter.ofPattern("MM-yyyy"));
            log.info("Last day of month detected. Finalizing leaderboard rewards for month: {}", monthYear);
            try {
                gamificationService.processMonthlyRewards(monthYear);
                log.info("Monthly leaderboard rewards finalized successfully for {}", monthYear);
            } catch (Exception e) {
                log.error("Failed to process monthly leaderboard rewards for {}", monthYear, e);
            }
        }
    }
}
