package com.nqd.nqd_lms_be.entity.knowledge;

import com.nqd.nqd_lms_be.entity.BaseEntity;
import com.nqd.nqd_lms_be.entity.Subject;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "knowledge_curriculums")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class KnowledgeCurriculum extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "grade_level", nullable = false, length = 50)
    private String gradeLevel;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "education_tier", length = 50)
    @Builder.Default
    private String educationTier = "Tiểu học";

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 1;

    @Column(name = "is_published")
    @Builder.Default
    private Boolean isPublished = true;

    @OneToMany(mappedBy = "curriculum", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("chapterOrder ASC")
    @Builder.Default
    private List<KnowledgeChapter> chapters = new ArrayList<>();
}
