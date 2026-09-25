package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(
    name = "classroom_materials",
    indexes = {
        @Index(name = "idx_classroom_materials_class", columnList = "classroom_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ClassroomMaterial extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * PDF, SLIDE, TEXTBOOK, EXAM_PREP, LINK, OTHER
     */
    @Column(name = "material_type", nullable = false, length = 50)
    @Builder.Default
    private String materialType = "PDF";

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private Integer downloadCount = 0;
}
