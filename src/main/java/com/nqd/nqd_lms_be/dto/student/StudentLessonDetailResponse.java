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
public class StudentLessonDetailResponse {
    private UUID id;
    private UUID chapterId;
    private String chapterTitle;
    private UUID courseId;
    private String courseName;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private Integer displayOrder;
    private Integer estimatedMinutes;
    private Boolean isPreview;
    private String videoUrl;
    private Boolean isLocked;
    private String lockReason;
}
