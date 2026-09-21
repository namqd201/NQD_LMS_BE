package com.nqd.nqd_lms_be.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProgressResponse {
    private UUID studentId;
    private long totalEnrolledCourses;
    private long completedCourses;
    private long totalExamsTaken;
    private long passedExams;
    private Double averageScore;
}
