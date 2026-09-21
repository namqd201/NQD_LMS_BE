package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.AiGradingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_grading_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class AiGradingRun extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_answer_id", nullable = false)
    private ExamAnswer examAnswer;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Column(name = "score", precision = 7, scale = 2)
    private BigDecimal score;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "rubric_result", columnDefinition = "jsonb")
    private String rubricResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AiGradingStatus status = AiGradingStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
