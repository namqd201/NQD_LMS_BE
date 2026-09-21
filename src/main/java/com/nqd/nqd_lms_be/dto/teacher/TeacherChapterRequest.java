package com.nqd.nqd_lms_be.dto.teacher;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherChapterRequest {
    @NotBlank(message = "Chapter title is required")
    private String title;

    private String description;
    private Integer displayOrder;
    private java.math.BigDecimal price;
    private Boolean isSellable;
}
