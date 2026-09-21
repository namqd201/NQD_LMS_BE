package com.nqd.nqd_lms_be.dto.student;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLessonProgressRequest {

    @DecimalMin(value = "0.0", message = "Progress percent cannot be negative")
    @DecimalMax(value = "100.0", message = "Progress percent cannot exceed 100")
    private BigDecimal progressPercent;

    private Boolean completed;

    private Boolean videoWatched;
}
