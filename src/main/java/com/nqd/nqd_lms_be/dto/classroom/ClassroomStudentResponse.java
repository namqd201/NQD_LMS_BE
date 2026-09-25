package com.nqd.nqd_lms_be.dto.classroom;

import com.nqd.nqd_lms_be.entity.enums.ClassEnrollmentStatus;
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
public class ClassroomStudentResponse {
    private UUID id;
    private UUID classroomId;
    private String classroomName;
    private String classroomCode;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private String studentAvatarUrl;
    private ClassEnrollmentStatus status;
    private LocalDateTime joinedAt;
    private String requestMessage;
    private LocalDateTime createdAt;
}
