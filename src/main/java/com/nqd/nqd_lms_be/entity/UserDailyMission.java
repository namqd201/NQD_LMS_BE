package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDate;

@Entity
@Table(
    name = "user_daily_missions",
    indexes = {
        @Index(name = "idx_daily_mission_user_date", columnList = "user_id, mission_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class UserDailyMission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "mission_date", nullable = false)
    private LocalDate missionDate;

    @Column(name = "mission_key", nullable = false, length = 50)
    private String missionKey;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "target_count", nullable = false)
    @Builder.Default
    private Integer targetCount = 1;

    @Column(name = "current_count", nullable = false)
    @Builder.Default
    private Integer currentCount = 0;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "is_claimed", nullable = false)
    @Builder.Default
    private Boolean isClaimed = false;

    @Column(name = "reward_xp", nullable = false)
    @Builder.Default
    private Integer rewardXp = 50;
}
