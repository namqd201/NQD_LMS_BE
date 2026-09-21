package com.nqd.nqd_lms_be.dto.teacher;

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
public class TeacherGradeAttemptRequest {
    @NotNull(message = "Total score is required")
    private BigDecimal totalScore;

    private String teacherFeedback;
}
