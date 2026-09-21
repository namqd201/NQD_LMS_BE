package com.nqd.nqd_lms_be.dto.teacher;

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
public class TeacherExamStudentCandidateResponse {
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private String avatarUrl;
    private UUID courseId;
    private String courseName;
    private Boolean isAssigned;
    private LocalDateTime assignedAt;
}
