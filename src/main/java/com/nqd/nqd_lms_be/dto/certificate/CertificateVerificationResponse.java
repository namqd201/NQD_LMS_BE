package com.nqd.nqd_lms_be.dto.certificate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateVerificationResponse {
    private boolean isValid;
    private String certificateCode;
    private String studentName;
    private String courseName;
    private String courseCode;
    private String subjectName;
    private LocalDateTime issuedAt;
    private LocalDateTime expiryDate;
    private boolean isRevoked;
    private String revocationReason;
    private String issuerName;
    private String verificationUrl;
}
