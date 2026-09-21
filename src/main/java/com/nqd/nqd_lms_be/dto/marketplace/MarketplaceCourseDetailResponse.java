package com.nqd.nqd_lms_be.dto.marketplace;

import com.nqd.nqd_lms_be.entity.enums.CoursePricingType;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.EnrollmentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceCourseDetailResponse {
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
    private String creatorBio;
    private LocalDateTime publishedAt;

    // Student specific access info
    private Boolean isEnrolled;
    private Boolean hasPurchased;
    private EnrollmentStatus enrollmentStatus;
    private Boolean isOwner;
    private Boolean isUltraMember;
    private BigDecimal proDiscountPrice;

    // Curriculum overview
    private List<MarketplaceChapterResponse> chapters;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MarketplaceChapterResponse {
        private UUID id;
        private String title;
        private String description;
        private Integer displayOrder;
        private BigDecimal price;
        private Boolean isSellable;
        private Boolean isPurchased;
        private List<MarketplaceLessonSummaryResponse> lessons;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MarketplaceLessonSummaryResponse {
        private UUID id;
        private String title;
        private String slug;
        private String summary;
        private Integer displayOrder;
        private Integer estimatedMinutes;
        private Boolean isPreview;
        private BigDecimal price;
        private Boolean isSellable;
        private Boolean isPurchased;
    }
}
