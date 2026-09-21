package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.ResourceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(
    name = "lesson_resources",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"lesson_id", "display_order"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class LessonResource extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 30)
    private ResourceType resourceType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 1;

    @Column(name = "is_preview", nullable = false)
    @Builder.Default
    private Boolean isPreview = false;
}
