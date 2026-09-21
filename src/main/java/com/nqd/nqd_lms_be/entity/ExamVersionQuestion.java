package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(
    name = "exam_version_questions",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"exam_version_id", "exam_question_id"}),
        @UniqueConstraint(columnNames = {"exam_version_id", "display_order"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ExamVersionQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_version_id", nullable = false)
    private ExamVersion examVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_question_id", nullable = false)
    private ExamQuestion examQuestion;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
