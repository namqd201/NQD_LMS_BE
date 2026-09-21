package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "exam_assignments",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"exam_id", "student_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ExamAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}
