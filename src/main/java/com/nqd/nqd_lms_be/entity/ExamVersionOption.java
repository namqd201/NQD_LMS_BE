package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(
    name = "exam_version_options",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"exam_version_question_id", "question_option_id"}),
        @UniqueConstraint(columnNames = {"exam_version_question_id", "display_order"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ExamVersionOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_version_question_id", nullable = false)
    private ExamVersionQuestion examVersionQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_option_id", nullable = false)
    private QuestionOption questionOption;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
