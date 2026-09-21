package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.ExerciseStatus;
import com.nqd.nqd_lms_be.entity.enums.ExerciseType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;

@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class Exercise extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @Builder.Default
    private ExerciseType type = ExerciseType.PRACTICE;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "passing_score", precision = 7, scale = 2)
    private BigDecimal passingScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExerciseStatus status = ExerciseStatus.DRAFT;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "show_explanation_immediately")
    @Builder.Default
    private Boolean showExplanationImmediately = true;

    @Column(name = "allow_retry")
    @Builder.Default
    private Boolean allowRetry = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User creator;
}

