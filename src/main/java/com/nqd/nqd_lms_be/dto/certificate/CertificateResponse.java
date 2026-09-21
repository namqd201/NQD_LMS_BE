package com.nqd.nqd_lms_be.dto.certificate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateResponse {
    private UUID id;
    private String certificateCode;
    private UUID courseId;
    private String courseName;
    private String courseCode;
    private String courseThumbnailUrl;
    private String subjectName;
    private String gradeLevel;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private String studentAvatarUrl;
    private LocalDateTime issuedAt;
    private LocalDateTime expiryDate;
    private Boolean isRevoked;
    private String revocationReason;
    private Double finalGrade;
    private String verificationUrl;
    private String downloadUrl;
}
