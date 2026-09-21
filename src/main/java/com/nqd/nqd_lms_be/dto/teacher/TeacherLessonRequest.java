package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.LessonStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherLessonRequest {
    @NotBlank(message = "Lesson title is required")
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
}
