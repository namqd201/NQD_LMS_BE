package com.nqd.nqd_lms_be.entity.knowledge;

import com.nqd.nqd_lms_be.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "knowledge_lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class KnowledgeLesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private KnowledgeChapter chapter;

    @Column(name = "lesson_order", nullable = false)
    private Integer lessonOrder;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", length = 255)
    private String slug;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "theory_markdown", columnDefinition = "TEXT")
    private String theoryMarkdown;

    @Column(name = "estimated_minutes")
    @Builder.Default
    private Integer estimatedMinutes = 40;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "PUBLISHED";

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    @Builder.Default
    private List<KnowledgeQuestion> questions = new ArrayList<>();
}
