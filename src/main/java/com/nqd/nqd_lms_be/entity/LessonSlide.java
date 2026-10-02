package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.SlideTargetType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.UUID;

@Entity
@Table(
    name = "lesson_slides",
    indexes = {
        @Index(name = "idx_lesson_slides_target", columnList = "target_type, target_id", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class LessonSlide extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    private SlideTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slide_url", columnDefinition = "TEXT")
    private String slideUrl;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "slide_content_json", columnDefinition = "TEXT")
    private String slideContentJson;

    @Column(name = "slide_source", nullable = false, length = 32)
    @Builder.Default
    private String slideSource = "AI_GENERATED"; // AI_GENERATED, MANUAL_UPLOAD

    @Column(name = "slide_count")
    @Builder.Default
    private Integer slideCount = 1;

    @Column(name = "creator_id")
    private UUID creatorId;
}
