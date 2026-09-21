package com.nqd.nqd_lms_be.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherBlueprintResponse {
    private UUID id;
    private UUID examId;
    private String name;
    private String description;
    private Integer totalQuestions;
    private BigDecimal totalMarks;
    private String configuration;
    private UUID creatorId;
    private LocalDateTime createdAt;
}
