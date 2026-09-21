package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(
    name = "question_options",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"question_id", "option_key"}),
        @UniqueConstraint(columnNames = {"question_id", "display_order"}),
        @UniqueConstraint(columnNames = {"question_id", "id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class QuestionOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_key", nullable = false, length = 10)
    private String optionKey;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
