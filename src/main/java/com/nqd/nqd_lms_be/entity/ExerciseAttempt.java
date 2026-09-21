package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.ExerciseAttemptStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "exercise_attempts",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"exercise_id", "student_id", "attempt_number"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ExerciseAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExerciseAttemptStatus status = ExerciseAttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "total_score", precision = 7, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "max_score", precision = 7, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "passed")
    private Boolean passed;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
}
