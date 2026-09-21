package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "certificates",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_certificate_course_student", columnNames = {"course_id", "student_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class Certificate extends BaseEntity {

    @Column(name = "certificate_code", nullable = false, unique = true, length = 64)
    private String certificateCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private CourseEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "is_revoked")
    @Builder.Default
    private Boolean isRevoked = false;

    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

    @Column(name = "final_grade")
    private Double finalGrade;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
        if (isRevoked == null) {
            isRevoked = false;
        }
    }
}
