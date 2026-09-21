package com.nqd.nqd_lms_be.dto.admin;

import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCourseResponse {
    private UUID id;
    private UUID subjectId;
    private String subjectName;
    private String name;
    private String code;
    private String description;
    private String gradeLevel;
    private String thumbnailUrl;
    private CourseStatus status;
    private Boolean isPrivate;
    private Boolean isDisabled;
    private String disabledReason;
    private com.nqd.nqd_lms_be.entity.enums.CoursePricingType pricingType;
    private java.math.BigDecimal price;
    private java.math.BigDecimal salePrice;
    private String currency;
    private LocalDateTime publishedAt;
    private String rejectReason;
    private Double averageRating;
    private Integer reviewCount;
    private UUID creatorId;
    private String creatorName;
    private String creatorEmail;
    private long enrolledStudentsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
