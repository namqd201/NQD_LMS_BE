package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.ClassInvitationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "classroom_invitations",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"classroom_id", "invited_email"})
    },
    indexes = {
        @Index(name = "idx_ci_email", columnList = "invited_email"),
        @Index(name = "idx_ci_classroom", columnList = "classroom_id"),
        @Index(name = "idx_ci_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ClassroomInvitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "invited_email", nullable = false)
    private String invitedEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ClassInvitationStatus status = ClassInvitationStatus.PENDING;

    @Column(name = "invitation_code", length = 64)
    private String invitationCode;

    @Column(name = "request_message", columnDefinition = "TEXT")
    private String requestMessage;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
