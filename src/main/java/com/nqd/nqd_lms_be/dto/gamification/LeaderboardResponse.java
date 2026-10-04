package com.nqd.nqd_lms_be.dto.gamification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {
    private String monthYear; // e.g. "Tháng 10/2026"
    private Integer daysRemainingInMonth;
    private List<LeaderboardItemResponse> topStudents;
    private LeaderboardItemResponse currentUserRank;
    private List<MonthlyRewardRuleDto> rewardRules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRewardRuleDto {
        private String rankRange; // "Top 1", "Top 2", "Top 3", "Top 4 - 10"
        private String rewardTitle; // "Gói Hội viên ULTRA 1 Tháng + Cúp Vàng Vinh danh"
        private String icon;
        private String highlightColor;
    }
}
