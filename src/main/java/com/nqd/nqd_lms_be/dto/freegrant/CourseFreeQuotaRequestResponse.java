package com.nqd.nqd_lms_be.dto.freegrant;

import com.nqd.nqd_lms_be.entity.enums.FreeQuotaRequestStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseFreeQuotaRequestResponse {
    private UUID id;
    private UUID courseId;
    private String courseName;
    private String courseCode;
    private UUID teacherId;
    private String teacherName;
    private String teacherEmail;
    private Integer requestedQuota;
    private String reason;
    private FreeQuotaRequestStatus status;
    private String adminNote;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
