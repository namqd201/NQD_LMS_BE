package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.LessonStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(
    name = "lessons",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chapter_id", "display_order"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class Lesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", length = 255)
    private String slug;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private LessonStatus status = LessonStatus.DRAFT;

    @Column(name = "is_preview", nullable = false)
    @Builder.Default
    private Boolean isPreview = false;

    @Column(name = "video_url", columnDefinition = "TEXT")
    private String videoUrl;

    @Column(name = "price", precision = 15, scale = 2)
    private java.math.BigDecimal price;

    @Column(name = "is_sellable", nullable = false)
    @Builder.Default
    private Boolean isSellable = false;
}
