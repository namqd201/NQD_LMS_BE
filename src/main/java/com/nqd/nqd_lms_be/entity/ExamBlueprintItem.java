package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;

@Entity
@Table(name = "exam_blueprint_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ExamBlueprintItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id", nullable = false)
    private ExamBlueprint blueprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private QuestionTag tag;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private QuestionDifficulty difficulty;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount;

    @Column(name = "marks_per_question", nullable = false, precision = 7, scale = 2)
    private BigDecimal marksPerQuestion;
}
