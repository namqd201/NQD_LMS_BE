package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.ValidationStatus;
import com.nqd.nqd_lms_be.entity.enums.ValidationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;

@Entity
@Table(name = "question_validations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class QuestionValidation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_type", nullable = false, length = 20)
    private ValidationType validationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ValidationStatus status;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private User validatedBy;
}
