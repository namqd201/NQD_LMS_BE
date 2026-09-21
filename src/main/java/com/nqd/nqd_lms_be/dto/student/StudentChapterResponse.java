package com.nqd.nqd_lms_be.dto.student;

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
public class StudentChapterResponse {
    private UUID id;
    private String title;
    private String description;
    private Integer displayOrder;
    private List<StudentLessonSummaryResponse> lessons;
}
