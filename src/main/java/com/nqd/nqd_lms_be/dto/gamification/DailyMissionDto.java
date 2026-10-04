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
public class DailyMissionDto {
    private UUID id;
    private String missionKey;
    private String title;
    private Integer targetCount;
    private Integer currentCount;
    private Boolean isCompleted;
    private Boolean isClaimed;
    private Integer rewardXp;
}
