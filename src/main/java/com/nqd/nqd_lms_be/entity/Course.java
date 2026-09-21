package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.CoursePricingType;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class Course extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false, length = 20)
    @Builder.Default
    private CoursePricingType pricingType = CoursePricingType.FREE;

    @Column(name = "price", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "sale_price", precision = 15, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "enrollment_count")
    @Builder.Default
    private Integer enrollmentCount = 0;

    @Column(name = "is_private", nullable = false)
    @Builder.Default
    private Boolean isPrivate = false;

    @Column(name = "is_disabled", nullable = false)
    @Builder.Default
    private Boolean isDisabled = false;

    @Column(name = "disabled_reason", columnDefinition = "TEXT")
    private String disabledReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User creator;

    public boolean isPublished() {
        return this.status != null && this.status.isPublished();
    }

    public boolean isPaid() {
        return this.pricingType == CoursePricingType.PAID || (this.price != null && this.price.compareTo(BigDecimal.ZERO) > 0);
    }

    public BigDecimal getEffectivePrice() {
        if (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) >= 0) {
            return salePrice;
        }
        return price != null ? price : BigDecimal.ZERO;
    }
}
