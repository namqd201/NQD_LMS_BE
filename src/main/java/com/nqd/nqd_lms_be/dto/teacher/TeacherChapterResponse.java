package com.nqd.nqd_lms_be.dto.teacher;

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
public class TeacherChapterResponse {
    private UUID id;
    private UUID courseId;
    private String title;
    private String description;
    private Integer displayOrder;
    private java.math.BigDecimal price;
    private Boolean isSellable;
    private List<TeacherLessonResponse> lessons;
}
