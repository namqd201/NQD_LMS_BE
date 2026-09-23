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
public class KnowledgeCurriculumResponse {
    private UUID id;
    private String code;
    private String title;
    private String description;
    private String gradeLevel;
    private String educationTier;
    private String subjectCode;
    private String subjectName;
    private String thumbnailUrl;
    private Integer totalChapters;
    private Integer totalLessons;
    private List<KnowledgeChapterResponse> chapters;
}
