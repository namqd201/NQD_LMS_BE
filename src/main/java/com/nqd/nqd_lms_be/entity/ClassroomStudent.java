package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.ClassEnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "classroom_students",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"classroom_id", "student_id"})
    },
    indexes = {
        @Index(name = "idx_cs_classroom", columnList = "classroom_id"),
        @Index(name = "idx_cs_student", columnList = "student_id"),
        @Index(name = "idx_cs_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ClassroomStudent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ClassEnrollmentStatus status = ClassEnrollmentStatus.PENDING_APPROVAL;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "request_message", columnDefinition = "TEXT")
    private String requestMessage;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.joinedAt == null && this.status == ClassEnrollmentStatus.ENROLLED) {
            this.joinedAt = LocalDateTime.now();
        }
    }
}
