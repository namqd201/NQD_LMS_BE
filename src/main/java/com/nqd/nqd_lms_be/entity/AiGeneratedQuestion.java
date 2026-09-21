package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.AiReviewStatus;
import com.nqd.nqd_lms_be.entity.enums.AiValidationStatus;
import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_generated_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class AiGeneratedQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private AiGenerationJob job;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private QuestionDifficulty difficulty;

    @Column(name = "marks", nullable = false, precision = 7, scale = 2)
    @Builder.Default
    private BigDecimal marks = new BigDecimal("1.00");

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "tags", length = 255)
    private String tags;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 20)
    @Builder.Default
    private AiValidationStatus validationStatus = AiValidationStatus.VALID;

    @Column(name = "validation_feedback", columnDefinition = "TEXT")
    private String validationFeedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    @Builder.Default
    private AiReviewStatus reviewStatus = AiReviewStatus.PENDING_REVIEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_question_id")
    private Question approvedQuestion;

    @OneToMany(mappedBy = "generatedQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<AiGeneratedOption> options = new ArrayList<>();
}
