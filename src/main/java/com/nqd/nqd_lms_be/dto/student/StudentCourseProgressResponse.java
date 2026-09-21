package com.nqd.nqd_lms_be.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseProgressResponse {
    private UUID courseId;
    private BigDecimal overallProgressPercent;
    private long totalLessons;
    private long completedLessons;
    private List<StudentLessonProgressResponse> lessonProgresses;
}
