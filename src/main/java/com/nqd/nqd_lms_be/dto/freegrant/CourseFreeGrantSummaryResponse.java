package com.nqd.nqd_lms_be.dto.freegrant;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseFreeGrantSummaryResponse {
    private UUID courseId;
    private String courseName;
    private int totalQuota;
    private int usedQuota;
    private int remainingQuota;
    private List<TeacherFreeGrantResponse> grants;
    private List<CourseFreeQuotaRequestResponse> quotaRequests;
}
