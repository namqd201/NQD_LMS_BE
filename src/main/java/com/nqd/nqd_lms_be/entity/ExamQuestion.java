package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;

@Entity
@Table(
    name = "exam_questions",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"exam_id", "question_id"}),
        @UniqueConstraint(columnNames = {"exam_id", "display_order"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ExamQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "marks", nullable = false, precision = 7, scale = 2)
    private BigDecimal marks;
}
