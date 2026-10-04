package com.nqd.nqd_lms_be.dto.gamification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardItemResponse {
    private Integer rank;
    private UUID userId;
    private String fullName;
    private String avatarUrl;
    private Long monthlyXp;
    private Long totalXp;
    private Integer currentStreak;
    private String rankTier;
    private String rankTierName;
    private Boolean isCurrentUser;
    private String rewardBadge; // "TOP 1: Gói Ultra + Cúp Vàng", etc.
}
