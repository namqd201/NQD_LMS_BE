package com.nqd.nqd_lms_be.dto.marketplace;

import com.nqd.nqd_lms_be.entity.enums.CoursePricingType;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceCourseCardResponse {
    private UUID id;
    private String name;
    private String code;
    private String description;
    private String gradeLevel;
    private String thumbnailUrl;
    private UUID subjectId;
    private String subjectName;
    private CourseStatus status;
    private CoursePricingType pricingType;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String currency;
    private Boolean isFree;
    private Double averageRating;
    private Integer reviewCount;
    private Integer enrollmentCount;
    private UUID creatorId;
    private String creatorName;
    private String creatorAvatarUrl;
    private LocalDateTime publishedAt;
}
