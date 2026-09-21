package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.TeacherApplicantType;
import com.nqd.nqd_lms_be.entity.enums.TeacherApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "teacher_applications",
    indexes = {
        @Index(name = "idx_teacher_app_user", columnList = "user_id"),
        @Index(name = "idx_teacher_app_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class TeacherApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicant_type", nullable = false, length = 50)
    private TeacherApplicantType applicantType;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "phone_number", nullable = false, length = 50)
    private String phoneNumber;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "institution_name", nullable = false, length = 255)
    private String institutionName;

    @Column(name = "major_or_subject", nullable = false, length = 255)
    private String majorOrSubject;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "document_urls", columnDefinition = "TEXT")
    private String documentUrls;

    @Column(name = "id_card_front_url", columnDefinition = "TEXT")
    private String idCardFrontUrl;

    @Column(name = "id_card_back_url", columnDefinition = "TEXT")
    private String idCardBackUrl;

    @Column(name = "sample_video_url", length = 500)
    private String sampleVideoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TeacherApplicationStatus status = TeacherApplicationStatus.PENDING;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
