package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.RankTier;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDate;

@Entity
@Table(
    name = "user_gamification",
    indexes = {
        @Index(name = "idx_gamification_user_id", columnList = "user_id", unique = true),
        @Index(name = "idx_gamification_monthly_xp", columnList = "monthly_xp DESC"),
        @Index(name = "idx_gamification_total_xp", columnList = "total_xp DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class UserGamification extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    @Builder.Default
    private Integer longestStreak = 0;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "total_xp", nullable = false)
    @Builder.Default
    private Long totalXp = 0L;

    @Column(name = "monthly_xp", nullable = false)
    @Builder.Default
    private Long monthlyXp = 0L;

    @Column(name = "today_xp", nullable = false)
    @Builder.Default
    private Long todayXp = 0L;

    @Column(name = "today_xp_date")
    private LocalDate todayXpDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "rank_tier", nullable = false, length = 30)
    @Builder.Default
    private RankTier rankTier = RankTier.BRONZE;

    @Column(name = "weekly_minutes", nullable = false)
    @Builder.Default
    private Integer weeklyMinutes = 0;
}
