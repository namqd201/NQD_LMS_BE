package com.nqd.nqd_lms_be.dto.classroom;

import com.nqd.nqd_lms_be.entity.enums.ClassEnrollmentStatus;
import com.nqd.nqd_lms_be.entity.enums.ClassroomStatus;
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
public class ClassroomResponse {
    private UUID id;
    private String name;
    private String code;
    private String description;
    private String gradeLevel;
    private UUID subjectId;
    private String subjectName;
    private UUID teacherId;
    private String teacherName;
    private String teacherEmail;
    private String teacherAvatarUrl;
    private Integer studentCount;
    private Long pendingRequestCount;
    private ClassroomStatus status;
    private String currentUserRole; // 'TEACHER', 'STUDENT', 'NONE'
    private ClassEnrollmentStatus currentUserEnrollmentStatus;
    private String coverImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
