package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherCourseRequest {
    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    @NotBlank(message = "Course name is required")
    private String name;

    @NotBlank(message = "Course code is required")
    private String code;

    private String description;
    private String gradeLevel;
    private String thumbnailUrl;
    private CourseStatus status;
    private Boolean isPrivate;
    private com.nqd.nqd_lms_be.entity.enums.CoursePricingType pricingType;
    private java.math.BigDecimal price;
    private java.math.BigDecimal salePrice;
    private String currency;
}
