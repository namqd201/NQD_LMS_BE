package com.nqd.nqd_lms_be.dto.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeLessonResponse {
    private UUID id;
    private Integer lessonOrder;
    private String title;
    private String slug;
    private String summary;
    private String theoryMarkdown;
    private Integer estimatedMinutes;
    private String status;
    private List<KnowledgeQuestionResponse> questions;
}
