package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "monthly_leaderboard_rewards",
    indexes = {
        @Index(name = "idx_monthly_rewards_month_rank", columnList = "month_year, rank_position"),
        @Index(name = "idx_monthly_rewards_user", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class MonthlyLeaderboardReward extends BaseEntity {

    @Column(name = "month_year", nullable = false, length = 20)
    private String monthYear; // e.g. "10-2026"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    @Column(name = "final_monthly_xp", nullable = false)
    private Long finalMonthlyXp;

    @Column(name = "final_streak", nullable = false)
    private Integer finalStreak;

    @Column(name = "reward_title", nullable = false, length = 255)
    private String rewardTitle;

    @Column(name = "reward_code", length = 100)
    private String rewardCode;

    @Column(name = "reward_status", nullable = false, length = 30)
    @Builder.Default
    private String rewardStatus = "DISTRIBUTED";

    @Column(name = "distributed_at")
    private LocalDateTime distributedAt;
}
