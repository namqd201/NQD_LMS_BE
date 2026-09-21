package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherEnrollmentResponse {
    private UUID enrollmentId;
    private UUID courseId;
    private String courseName;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private EnrollmentStatus status;
    private LocalDateTime enrolledAt;
}
