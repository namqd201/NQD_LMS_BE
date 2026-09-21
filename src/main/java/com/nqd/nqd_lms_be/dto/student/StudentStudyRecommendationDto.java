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
public class StudentStudyRecommendationDto {
    private UUID courseId;
    private String courseTitle;
    private UUID lessonId;
    private String lessonTitle;
    private String topic;
    private String reason;
    private String priority; // "HIGH", "MEDIUM", "LOW"
    private Double currentScoreAverage;
}
