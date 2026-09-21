package com.nqd.nqd_lms_be.dto.teacher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherBlueprintRequest {
    @NotBlank(message = "Blueprint name is required")
    private String name;

    private String description;

    @NotNull(message = "Total questions is required")
    private Integer totalQuestions;

    @NotNull(message = "Total marks is required")
    private BigDecimal totalMarks;

    private String configuration;
}
