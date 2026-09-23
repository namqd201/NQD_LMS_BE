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
public class KnowledgeChapterResponse {
    private UUID id;
    private Integer chapterOrder;
    private String title;
    private String description;
    private List<KnowledgeLessonResponse> lessons;
}
