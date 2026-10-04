package com.nqd.nqd_lms_be.service.gamification;

import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.gamification.*;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.DiscountType;
import com.nqd.nqd_lms_be.entity.enums.GamificationActionType;
import com.nqd.nqd_lms_be.entity.enums.RankTier;
import com.nqd.nqd_lms_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationServiceImpl implements GamificationService {

    private final UserGamificationRepository userGamificationRepository;
    private final GamificationXpLogRepository xpLogRepository;
    private final UserDailyMissionRepository dailyMissionRepository;
    private final MonthlyLeaderboardRewardRepository monthlyRewardRepository;
    private final UserRepository userRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CouponRepository couponRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public GamificationDashboardResponse getStudentDashboard(UUID studentId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));

        UserGamification gamification = getOrCreateGamification(user);
        LocalDate today = LocalDate.now();

        // 1. Sync daily reset of todayXp if date changed
        if (gamification.getTodayXpDate() == null || !gamification.getTodayXpDate().equals(today)) {
            gamification.setTodayXp(0L);
            gamification.setTodayXpDate(today);
            userGamificationRepository.save(gamification);
        }

        // 2. Ensure daily missions for today exist
        List<UserDailyMission> missions = ensureDailyMissions(user, today);

        // Auto-check streak mission if user already studied today
        boolean hasStudiedToday = today.equals(gamification.getLastActivityDate());
        for (UserDailyMission m : missions) {
            if ("MAINTAIN_STREAK".equals(m.getMissionKey()) && hasStudiedToday && !Boolean.TRUE.equals(m.getIsCompleted())) {
                m.setCurrentCount(1);
                m.setIsCompleted(true);
                dailyMissionRepository.save(m);
            }
        }

        List<DailyMissionDto> missionDtos = missions.stream()
                .map(m -> DailyMissionDto.builder()
                        .id(m.getId())
                        .missionKey(m.getMissionKey())
                        .title(m.getTitle())
                        .targetCount(m.getTargetCount())
                        .currentCount(m.getCurrentCount())
                        .isCompleted(m.getIsCompleted())
                        .isClaimed(m.getIsClaimed())
                        .rewardXp(m.getRewardXp())
                        .build())
                .collect(Collectors.toList());

        int missionsCompleted = (int) missionDtos.stream().filter(m -> Boolean.TRUE.equals(m.getIsCompleted())).count();
        int missionsTotal = missionDtos.size();
        int completionPercent = missionsTotal > 0 ? (missionsCompleted * 100 / missionsTotal) : 0;
        boolean allClaimed = missionDtos.stream().allMatch(m -> Boolean.TRUE.equals(m.getIsClaimed()));

        String nextMissionTitle = missionDtos.stream()
                .filter(m -> !Boolean.TRUE.equals(m.getIsCompleted()))
                .map(DailyMissionDto::getTitle)
                .findFirst()
                .orElse("Tất cả nhiệm vụ hôm nay đã hoàn thành!");

        // 3. Weekly Streak status (Monday to Sunday)
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<java.sql.Date> activityDatesSql = lessonProgressRepository.findDistinctActivityDatesByStudentId(studentId);
        Set<LocalDate> activityDates = activityDatesSql != null
                ? activityDatesSql.stream().map(java.sql.Date::toLocalDate).collect(Collectors.toSet())
                : new HashSet<>();

        if (hasStudiedToday) {
            activityDates.add(today);
        }

        List<Boolean> weeklyStreakStatus = new ArrayList<>();
        List<Double> dailyStudyHours = new ArrayList<>();
        double totalWeeklyHours = 0.0;

        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            boolean studied = activityDates.contains(d);
            weeklyStreakStatus.add(studied);

            // Estimated study time based on completed activity (approx 1.5h - 3h if active)
            double hours = studied ? (d.equals(today) ? 2.5 : 2.0) : 0.0;
            dailyStudyHours.add(hours);
            totalWeeklyHours += hours;
        }

        // 4. Compute monthly rank
        long monthlyRank = userGamificationRepository.findMonthlyRank(gamification.getMonthlyXp());

        return GamificationDashboardResponse.builder()
                .studentId(user.getId())
                .studentName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                // Metric 1: Study time
                .studyHoursThisWeek(Math.round(totalWeeklyHours * 10.0) / 10.0)
                .studyTargetHours(18.0)
                .studyGrowthPercent(15.4)
                .dailyStudyHoursOfWeek(dailyStudyHours)
                // Metric 2: Missions
                .dailyMissionsCompleted(missionsCompleted)
                .dailyMissionsTotal(missionsTotal)
                .dailyCompletionPercent(completionPercent)
                .dailyMissions(missionDtos)
                .dailyRewardClaimed(allClaimed)
                .nextMissionTitle(nextMissionTitle)
                // Metric 3: Streak
                .currentStreak(gamification.getCurrentStreak())
                .longestStreak(gamification.getLongestStreak())
                .hasStudiedToday(hasStudiedToday)
                .weeklyStreakStatus(weeklyStreakStatus)
                // Metric 4: XP
                .totalXp(gamification.getTotalXp())
                .monthlyXp(gamification.getMonthlyXp())
                .todayXp(gamification.getTodayXp())
                .monthlyRank(monthlyRank)
                .rankTier(gamification.getRankTier().name())
                .rankTierName(gamification.getRankTier().getDisplayName())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LeaderboardResponse getMonthlyLeaderboard(UUID currentUserId, String monthYear) {
        if (monthYear == null || monthYear.isBlank()) {
            monthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy"));
        }

        List<UserGamification> topGamification = userGamificationRepository.findMonthlyLeaderboard(PageRequest.of(0, 50));
        List<LeaderboardItemResponse> items = new ArrayList<>();
        LeaderboardItemResponse currentUserItem = null;

        int rank = 1;
        for (UserGamification g : topGamification) {
            boolean isCurrent = currentUserId != null && g.getUser().getId().equals(currentUserId);
            String rewardBadge = getRankRewardBadge(rank);

            LeaderboardItemResponse item = LeaderboardItemResponse.builder()
                    .rank(rank)
                    .userId(g.getUser().getId())
                    .fullName(g.getUser().getFullName())
                    .avatarUrl(g.getUser().getAvatarUrl())
                    .monthlyXp(g.getMonthlyXp())
                    .totalXp(g.getTotalXp())
                    .currentStreak(g.getCurrentStreak())
                    .rankTier(g.getRankTier().name())
                    .rankTierName(g.getRankTier().getDisplayName())
                    .isCurrentUser(isCurrent)
                    .rewardBadge(rewardBadge)
                    .build();

            items.add(item);
            if (isCurrent) {
                currentUserItem = item;
            }
            rank++;
        }

        // If current user is not in top 50, compute their stand-alone rank
        if (currentUserId != null && currentUserItem == null) {
            Optional<UserGamification> myOpt = userGamificationRepository.findByUserId(currentUserId);
            if (myOpt.isPresent()) {
                UserGamification mg = myOpt.get();
                long myRank = userGamificationRepository.findMonthlyRank(mg.getMonthlyXp());
                currentUserItem = LeaderboardItemResponse.builder()
                        .rank((int) myRank)
                        .userId(mg.getUser().getId())
                        .fullName(mg.getUser().getFullName())
                        .avatarUrl(mg.getUser().getAvatarUrl())
                        .monthlyXp(mg.getMonthlyXp())
                        .totalXp(mg.getTotalXp())
                        .currentStreak(mg.getCurrentStreak())
                        .rankTier(mg.getRankTier().name())
                        .rankTierName(mg.getRankTier().getDisplayName())
                        .isCurrentUser(true)
                        .rewardBadge(getRankRewardBadge((int) myRank))
                        .build();
            }
        }

        // Days remaining in current month
        LocalDate today = LocalDate.now();
        LocalDate endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());
        int daysRemaining = Math.max(0, endOfMonth.getDayOfMonth() - today.getDayOfMonth());

        return LeaderboardResponse.builder()
                .monthYear("Tháng " + monthYear)
                .daysRemainingInMonth(daysRemaining)
                .topStudents(items)
                .currentUserRank(currentUserItem)
                .rewardRules(getMonthlyRewardRules())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LeaderboardResponse getAllTimeLeaderboard(UUID currentUserId) {
        List<UserGamification> topGamification = userGamificationRepository.findAllTimeLeaderboard(PageRequest.of(0, 50));
        List<LeaderboardItemResponse> items = new ArrayList<>();
        LeaderboardItemResponse currentUserItem = null;

        int rank = 1;
        for (UserGamification g : topGamification) {
            boolean isCurrent = currentUserId != null && g.getUser().getId().equals(currentUserId);
            LeaderboardItemResponse item = LeaderboardItemResponse.builder()
                    .rank(rank)
                    .userId(g.getUser().getId())
                    .fullName(g.getUser().getFullName())
                    .avatarUrl(g.getUser().getAvatarUrl())
                    .monthlyXp(g.getMonthlyXp())
                    .totalXp(g.getTotalXp())
                    .currentStreak(g.getCurrentStreak())
                    .rankTier(g.getRankTier().name())
                    .rankTierName(g.getRankTier().getDisplayName())
                    .isCurrentUser(isCurrent)
                    .rewardBadge(getRankRewardBadge(rank))
                    .build();

            items.add(item);
            if (isCurrent) {
                currentUserItem = item;
            }
            rank++;
        }

        return LeaderboardResponse.builder()
                .monthYear("Mọi thời đại (All-Time)")
                .daysRemainingInMonth(0)
                .topStudents(items)
                .currentUserRank(currentUserItem)
                .rewardRules(getMonthlyRewardRules())
                .build();
    }

    @Override
    @Transactional
    public ClaimMissionResponse claimDailyMission(UUID studentId, UUID missionId) {
        UserDailyMission mission = dailyMissionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission", missionId));

        if (!mission.getUser().getId().equals(studentId)) {
            throw new IllegalArgumentException("Nhiệm vụ không thuộc về người dùng này.");
        }

        if (!Boolean.TRUE.equals(mission.getIsCompleted())) {
            throw new IllegalStateException("Nhiệm vụ chưa hoàn thành!");
        }

        if (Boolean.TRUE.equals(mission.getIsClaimed())) {
            throw new IllegalStateException("Bạn đã nhận phần thưởng cho nhiệm vụ này rồi!");
        }

        mission.setIsClaimed(true);
        dailyMissionRepository.save(mission);

        UserGamification gamification = getOrCreateGamification(mission.getUser());
        addXp(gamification, mission.getRewardXp(), GamificationActionType.DAILY_MISSION,
                mission.getId().toString(), "Hoàn thành nhiệm vụ: " + mission.getTitle());

        return ClaimMissionResponse.builder()
                .success(true)
                .message("Chúc mừng! Bạn đã nhận thành công +" + mission.getRewardXp() + " XP!")
                .xpClaimed(mission.getRewardXp())
                .newTotalXp(gamification.getTotalXp())
                .newMonthlyXp(gamification.getMonthlyXp())
                .newTodayXp(gamification.getTodayXp())
                .build();
    }

    @Override
    @Transactional
    public void onLessonCompleted(UUID studentId, UUID lessonId) {
        User user = userRepository.findById(studentId).orElse(null);
        if (user == null) return;

        UserGamification gamification = getOrCreateGamification(user);
        recordActivity(gamification);

        // Award +10 XP for lesson completion
        addXp(gamification, 10, GamificationActionType.LESSON_COMPLETED,
                lessonId.toString(), "Hoàn thành bài học");

        // Advance mission
        advanceDailyMission(user, "WATCH_LESSON", 1);
    }

    @Override
    @Transactional
    public void onExerciseCompleted(UUID studentId, UUID exerciseId, boolean passed, BigDecimal scorePercentage) {
        User user = userRepository.findById(studentId).orElse(null);
        if (user == null) return;

        UserGamification gamification = getOrCreateGamification(user);
        recordActivity(gamification);

        int xp = passed ? 20 : 5;
        addXp(gamification, xp, GamificationActionType.EXERCISE_PASSED,
                exerciseId.toString(), "Làm bài tập: " + (passed ? "Đạt 100%" : "Đã hoàn thành"));

        if (passed) {
            advanceDailyMission(user, "COMPLETE_EXERCISE", 1);
        }
    }

    @Override
    @Transactional
    public void onExamSubmitted(UUID studentId, UUID examId, boolean passed, BigDecimal scorePercentage) {
        User user = userRepository.findById(studentId).orElse(null);
        if (user == null) return;

        UserGamification gamification = getOrCreateGamification(user);
        recordActivity(gamification);

        int xp = 30;
        if (scorePercentage != null && scorePercentage.compareTo(new BigDecimal("80.0")) >= 0) {
            xp += 20; // Bonus for high score >= 80%
        }

        addXp(gamification, xp, GamificationActionType.EXAM_SUBMITTED,
                examId.toString(), "Nộp bài kiểm tra/thi");
    }

    @Override
    @Transactional
    public void processDailyStreakResets() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        // Find users whose last activity was before yesterday but streak > 0 -> Streak broken!
        List<UserGamification> broken = userGamificationRepository.findBrokenStreaks(yesterday);
        for (UserGamification g : broken) {
            log.info("Resetting broken streak for user {}. Previous streak was {}", g.getUser().getId(), g.getCurrentStreak());
            g.setCurrentStreak(0);
        }
        userGamificationRepository.saveAll(broken);
    }

    @Override
    @Transactional
    public void processMonthlyRewards(String targetMonthYear) {
        if (targetMonthYear == null || targetMonthYear.isBlank()) {
            targetMonthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-yyyy"));
        }

        if (monthlyRewardRepository.existsByMonthYear(targetMonthYear)) {
            log.warn("Monthly rewards for {} already processed. Skipping.", targetMonthYear);
            return;
        }

        List<UserGamification> topStudents = userGamificationRepository.findMonthlyLeaderboard(PageRequest.of(0, 10));
        LocalDateTime now = LocalDateTime.now();

        int rank = 1;
        for (UserGamification g : topStudents) {
            User user = g.getUser();
            String rewardTitle;
            String couponCode = null;

            if (rank == 1) {
                rewardTitle = "Vinh danh Quán quân Tháng: Tặng 1 Tháng Hội viên ULTRA + Cúp Vàng";
                couponCode = createMonthlyWinnerCoupon(user, 100, "VOUCHER-TOP1-" + System.currentTimeMillis() % 100000);
            } else if (rank == 2) {
                rewardTitle = "Vinh danh Á quân 1 Tháng: Tặng Voucher giảm 70% Khóa học + Cúp Bạc";
                couponCode = createMonthlyWinnerCoupon(user, 70, "VOUCHER-TOP2-" + System.currentTimeMillis() % 100000);
            } else if (rank == 3) {
                rewardTitle = "Vinh danh Á quân 2 Tháng: Tặng Voucher giảm 50% Khóa học + Cúp Đồng";
                couponCode = createMonthlyWinnerCoupon(user, 50, "VOUCHER-TOP3-" + System.currentTimeMillis() % 100000);
            } else {
                rewardTitle = "Top 10 Năng Động: Thưởng 500 XP + Voucher giảm 20%";
                couponCode = createMonthlyWinnerCoupon(user, 20, "VOUCHER-TOP10-" + System.currentTimeMillis() % 100000);
                addXp(g, 500, GamificationActionType.STREAK_BONUS, targetMonthYear, "Thưởng Top 10 Năng Động Tháng " + targetMonthYear);
            }

            MonthlyLeaderboardReward reward = MonthlyLeaderboardReward.builder()
                    .monthYear(targetMonthYear)
                    .user(user)
                    .rankPosition(rank)
                    .finalMonthlyXp(g.getMonthlyXp())
                    .finalStreak(g.getCurrentStreak())
                    .rewardTitle(rewardTitle)
                    .rewardCode(couponCode)
                    .rewardStatus("DISTRIBUTED")
                    .distributedAt(now)
                    .build();
            monthlyRewardRepository.save(reward);

            // Send notification to user
            Notification notification = Notification.builder()
                    .user(user)
                    .type("MONTHLY_LEADERBOARD_REWARD")
                    .title("🏆 Chúc mừng bạn đạt Rank #" + rank + " Bảng Xếp Hạng Tháng " + targetMonthYear + "!")
                    .body("Bạn đã nhận được: " + rewardTitle + (couponCode != null ? ". Mã Voucher của bạn: " + couponCode : ""))
                    .linkUrl("/leaderboard")
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);

            rank++;
        }

        // Reset monthly XP for all users for next month
        userGamificationRepository.resetAllMonthlyXp();
        log.info("Finished monthly rewards distribution for month {}", targetMonthYear);
    }

    // --- Private Helper Methods ---

    private UserGamification getOrCreateGamification(User user) {
        return userGamificationRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    // Try to initialize streak from historical lesson progress
                    List<java.sql.Date> dateRows = lessonProgressRepository.findDistinctActivityDatesByStudentId(user.getId());
                    int initialStreak = 0;
                    LocalDate lastDate = null;

                    if (dateRows != null && !dateRows.isEmpty()) {
                        List<LocalDate> sorted = dateRows.stream()
                                .map(java.sql.Date::toLocalDate)
                                .distinct()
                                .sorted(Comparator.reverseOrder())
                                .collect(Collectors.toList());

                        LocalDate today = LocalDate.now();
                        LocalDate yesterday = today.minusDays(1);
                        LocalDate mostRecent = sorted.get(0);
                        lastDate = mostRecent;

                        if (mostRecent.equals(today) || mostRecent.equals(yesterday)) {
                            LocalDate expected = mostRecent;
                            for (LocalDate d : sorted) {
                                if (d.equals(expected)) {
                                    initialStreak++;
                                    expected = expected.minusDays(1);
                                } else {
                                    break;
                                }
                            }
                        }
                    }

                    UserGamification g = UserGamification.builder()
                            .user(user)
                            .currentStreak(initialStreak)
                            .longestStreak(initialStreak)
                            .lastActivityDate(lastDate)
                            .totalXp(initialStreak * 15L)
                            .monthlyXp(initialStreak * 15L)
                            .todayXp(0L)
                            .todayXpDate(LocalDate.now())
                            .rankTier(RankTier.fromTotalXp(initialStreak * 15L))
                            .weeklyMinutes(0)
                            .build();

                    return userGamificationRepository.save(g);
                });
    }

    private void recordActivity(UserGamification g) {
        LocalDate today = LocalDate.now();
        LocalDate lastDate = g.getLastActivityDate();

        if (lastDate == null) {
            g.setCurrentStreak(1);
            g.setLongestStreak(Math.max(g.getLongestStreak(), 1));
            g.setLastActivityDate(today);
            addXp(g, 15, GamificationActionType.STREAK_BONUS, today.toString(), "Khởi động chuỗi học tập mới (+15 XP)");
        } else if (lastDate.equals(today)) {
            // Already counted today
            return;
        } else if (lastDate.equals(today.minusDays(1))) {
            // Consecutive day!
            g.setCurrentStreak(g.getCurrentStreak() + 1);
            g.setLongestStreak(Math.max(g.getLongestStreak(), g.getCurrentStreak()));
            g.setLastActivityDate(today);
            addXp(g, 15, GamificationActionType.STREAK_BONUS, today.toString(), "Duy trì chuỗi học tập liên tục ngày thứ " + g.getCurrentStreak() + " (+15 XP)");

            // Milestone rewards
            if (g.getCurrentStreak() == 7) {
                addXp(g, 50, GamificationActionType.STREAK_BONUS, "MILESTONE_7", "Thưởng mốc Streak 7 ngày (+50 XP)");
            } else if (g.getCurrentStreak() == 14) {
                addXp(g, 120, GamificationActionType.STREAK_BONUS, "MILESTONE_14", "Thưởng mốc Streak 14 ngày (+120 XP)");
            } else if (g.getCurrentStreak() == 30) {
                addXp(g, 300, GamificationActionType.STREAK_BONUS, "MILESTONE_30", "Thưởng mốc Streak 30 ngày (+300 XP)");
            }
        } else {
            // Gap > 1 day -> Reset streak to 1
            g.setCurrentStreak(1);
            g.setLastActivityDate(today);
            addXp(g, 15, GamificationActionType.STREAK_BONUS, today.toString(), "Bắt đầu lại chuỗi học tập mới (+15 XP)");
        }

        userGamificationRepository.save(g);
    }

    private void addXp(UserGamification g, int amount, GamificationActionType type, String refId, String desc) {
        LocalDate today = LocalDate.now();
        g.setTotalXp(g.getTotalXp() + amount);
        g.setMonthlyXp(g.getMonthlyXp() + amount);

        if (g.getTodayXpDate() == null || !g.getTodayXpDate().equals(today)) {
            g.setTodayXp((long) amount);
            g.setTodayXpDate(today);
        } else {
            g.setTodayXp(g.getTodayXp() + amount);
        }

        g.setRankTier(RankTier.fromTotalXp(g.getTotalXp()));
        userGamificationRepository.save(g);

        GamificationXpLog logEntry = GamificationXpLog.builder()
                .user(g.getUser())
                .xpAmount(amount)
                .actionType(type)
                .referenceId(refId)
                .description(desc)
                .build();
        xpLogRepository.save(logEntry);
    }

    private List<UserDailyMission> ensureDailyMissions(User user, LocalDate date) {
        List<UserDailyMission> existing = dailyMissionRepository.findByUserIdAndMissionDate(user.getId(), date);
        if (!existing.isEmpty()) {
            return existing;
        }

        List<UserDailyMission> newMissions = List.of(
                UserDailyMission.builder()
                        .user(user)
                        .missionDate(date)
                        .missionKey("WATCH_LESSON")
                        .title("Xem và hoàn thành 1 bài giảng")
                        .targetCount(1)
                        .currentCount(0)
                        .isCompleted(false)
                        .isClaimed(false)
                        .rewardXp(20)
                        .build(),
                UserDailyMission.builder()
                        .user(user)
                        .missionDate(date)
                        .missionKey("COMPLETE_EXERCISE")
                        .title("Giải 1 bài tập trắc nghiệm đạt 100%")
                        .targetCount(1)
                        .currentCount(0)
                        .isCompleted(false)
                        .isClaimed(false)
                        .rewardXp(30)
                        .build(),
                UserDailyMission.builder()
                        .user(user)
                        .missionDate(date)
                        .missionKey("MAINTAIN_STREAK")
                        .title("Điểm danh học tập giữ chuỗi hôm nay")
                        .targetCount(1)
                        .currentCount(0)
                        .isCompleted(false)
                        .isClaimed(false)
                        .rewardXp(15)
                        .build()
        );

        return dailyMissionRepository.saveAll(newMissions);
    }

    private void advanceDailyMission(User user, String missionKey, int count) {
        LocalDate today = LocalDate.now();
        dailyMissionRepository.findByUserIdAndMissionDateAndMissionKey(user.getId(), today, missionKey)
                .ifPresent(m -> {
                    if (!Boolean.TRUE.equals(m.getIsCompleted())) {
                        m.setCurrentCount(m.getCurrentCount() + count);
                        if (m.getCurrentCount() >= m.getTargetCount()) {
                            m.setIsCompleted(true);
                        }
                        dailyMissionRepository.save(m);
                    }
                });
    }

    private String getRankRewardBadge(int rank) {
        if (rank == 1) return "🥇 Quán quân: Cúp Vàng + Hội viên ULTRA";
        if (rank == 2) return "🥈 Á quân: Cúp Bạc + Voucher 70%";
        if (rank == 3) return "🥉 Hạng 3: Cúp Đồng + Voucher 50%";
        if (rank <= 10) return "⭐ Top 10: Thưởng 500 XP + Voucher 20%";
        return null;
    }

    private List<LeaderboardResponse.MonthlyRewardRuleDto> getMonthlyRewardRules() {
        return List.of(
                LeaderboardResponse.MonthlyRewardRuleDto.builder()
                        .rankRange("Top 1 Quán Quân")
                        .rewardTitle("1 Tháng Hội viên ULTRA (hoặc Voucher 100% Khóa học) + Cúp Vàng Vinh Danh")
                        .icon("🏆")
                        .highlightColor("amber")
                        .build(),
                LeaderboardResponse.MonthlyRewardRuleDto.builder()
                        .rankRange("Top 2 Á Quân")
                        .rewardTitle("Voucher Giảm 70% Tất cả Khóa học + Cúp Bạc Vinh Danh")
                        .icon("🥈")
                        .highlightColor("slate")
                        .build(),
                LeaderboardResponse.MonthlyRewardRuleDto.builder()
                        .rankRange("Top 3")
                        .rewardTitle("Voucher Giảm 50% Tất cả Khóa học + Cúp Đồng Vinh Danh")
                        .icon("🥉")
                        .highlightColor("orange")
                        .build(),
                LeaderboardResponse.MonthlyRewardRuleDto.builder()
                        .rankRange("Top 4 - 10")
                        .rewardTitle("Cộng +500 XP Năng Động + Voucher Giảm 20% Khóa học")
                        .icon("⭐")
                        .highlightColor("indigo")
                        .build()
        );
    }

    private String createMonthlyWinnerCoupon(User user, int discountPercent, String code) {
        try {
            Coupon coupon = Coupon.builder()
                    .code(code)
                    .name("Phần thưởng Bảng Xếp Hạng LMS (" + discountPercent + "%) cho " + user.getFullName())
                    .description("Voucher phần thưởng vinh danh dành riêng cho học viên đạt thứ hạng cao trong tháng.")
                    .discountType(DiscountType.PERCENTAGE)
                    .discountValue(BigDecimal.valueOf(discountPercent))
                    .minOrderAmount(BigDecimal.ZERO)
                    .maxDiscountAmount(BigDecimal.valueOf(2000000))
                    .usageLimit(1)
                    .usedCount(0)
                    .validFrom(LocalDateTime.now())
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .build();
            couponRepository.save(coupon);
            return code;
        } catch (Exception e) {
            log.error("Failed to create reward coupon for user {}", user.getId(), e);
            return null;
        }
    }
}
