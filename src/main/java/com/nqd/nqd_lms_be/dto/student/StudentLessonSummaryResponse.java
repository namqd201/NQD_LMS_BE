package com.nqd.nqd_lms_be.dto.student;

import com.nqd.nqd_lms_be.entity.enums.LessonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentLessonSummaryResponse {
    private UUID id;
    private String title;
    private String slug;
    private String summary;
    private Integer displayOrder;
    private Integer estimatedMinutes;
    private LessonStatus status;
    private Boolean isPreview;
    private Boolean isLocked;
    private String lockReason;
    private Boolean isCompleted;
    private Boolean isExercisesPassed;
}
