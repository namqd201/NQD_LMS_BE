package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.ExamAttemptStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "exam_attempts",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"exam_id", "student_id", "attempt_number"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ExamAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_version_id")
    private ExamVersion examVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExamAttemptStatus status = ExamAttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "total_score", precision = 7, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "passed")
    private Boolean passed;

    @Column(name = "violation_count", nullable = false)
    @Builder.Default
    private Integer violationCount = 0;

    @Column(name = "is_flagged", nullable = false)
    @Builder.Default
    private Boolean isFlagged = false;

    @Column(name = "flag_reason", columnDefinition = "TEXT")
    private String flagReason;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
}
