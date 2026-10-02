package com.nqd.nqd_lms_be.dto.classroom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClassroomStatsResponse {
    private long totalClassrooms;
    private long activeClassrooms;
    private long archivedClassrooms;
    private long totalStudents;
    private long totalLiveNow;
}
