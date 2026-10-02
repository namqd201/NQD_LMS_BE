package com.nqd.nqd_lms_be.dto.freegrant;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherFreeGrantResponse {
    private UUID enrollmentId;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private LocalDateTime grantedAt;
    private UUID courseId;
    private String courseName;
}
