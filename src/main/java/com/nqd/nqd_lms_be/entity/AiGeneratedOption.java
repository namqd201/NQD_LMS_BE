package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = "ai_generated_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class AiGeneratedOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_question_id", nullable = false)
    private AiGeneratedQuestion generatedQuestion;

    @Column(name = "option_key", nullable = false, length = 10)
    private String optionKey;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 1;
}
