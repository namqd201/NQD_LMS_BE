package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
    name = "exercise_questions",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"exercise_id", "display_order"})
    }
)
@IdClass(ExerciseQuestionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ExerciseQuestion extends BaseIdEntity {

    @Id
    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Id
    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", insertable = false, updatable = false)
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", insertable = false, updatable = false)
    private Question question;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "marks", nullable = false, precision = 7, scale = 2)
    private BigDecimal marks;
}
