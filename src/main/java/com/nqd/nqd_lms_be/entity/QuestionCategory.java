package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.QuestionCategoryVisibility;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = "question_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class QuestionCategory extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private QuestionCategory parent;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 30)
    @Builder.Default
    private QuestionCategoryVisibility visibility = QuestionCategoryVisibility.TEACHER_SHARED;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;
}
