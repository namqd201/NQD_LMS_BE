package com.nqd.nqd_lms_be.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAiTutorResponse {
    private UUID conversationId;
    private String conversationTitle;
    private String answer;
    private String hint;
    private String context;
    private String contextSummary;
    private StudentAiTutorMode mode;
    private Integer remainingRequests;
    @Builder.Default
    private List<StudentStudyRecommendationDto> recommendations = new ArrayList<>();
}
