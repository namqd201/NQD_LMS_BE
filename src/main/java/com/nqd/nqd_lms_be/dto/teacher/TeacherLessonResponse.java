package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.LessonStatus;
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
public class TeacherLessonResponse {
    private UUID id;
    private UUID chapterId;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private Integer displayOrder;
    private Integer estimatedMinutes;
    private LessonStatus status;
    private Boolean isPreview;
    private String videoUrl;
    private java.math.BigDecimal price;
    private Boolean isSellable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
