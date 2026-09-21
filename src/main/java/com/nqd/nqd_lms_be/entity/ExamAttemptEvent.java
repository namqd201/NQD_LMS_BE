package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.ExamAttemptEventType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_attempt_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ExamAttemptEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private ExamAttemptEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "is_violation", nullable = false)
    @Builder.Default
    private Boolean isViolation = true;
}
