package com.nqd.nqd_lms_be.dto.gamification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimMissionResponse {
    private Boolean success;
    private String message;
    private Integer xpClaimed;
    private Long newTotalXp;
    private Long newMonthlyXp;
    private Long newTodayXp;
}
