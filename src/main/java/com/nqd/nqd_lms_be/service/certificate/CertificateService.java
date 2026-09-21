package com.nqd.nqd_lms_be.service.certificate;

import com.nqd.nqd_lms_be.dto.certificate.CertificateResponse;
import com.nqd.nqd_lms_be.dto.certificate.CertificateVerificationResponse;

import java.util.List;
import java.util.UUID;

public interface CertificateService {

    CertificateResponse issueCertificate(UUID studentId, UUID courseId);

    com.nqd.nqd_lms_be.dto.certificate.CertificateEligibilityResponse checkEligibility(UUID studentId, UUID courseId);

    java.util.Optional<CertificateResponse> checkAndAutoIssueCertificate(UUID studentId, UUID courseId);

    List<CertificateResponse> getMyCertificates(UUID studentId);

    CertificateResponse getCertificateById(UUID certificateId, UUID currentUserId, boolean isPrivileged);

    CertificateVerificationResponse verifyCertificate(String certificateCode);

    byte[] generateCertificatePdf(UUID certificateId, UUID currentUserId, boolean isPrivileged);

    CertificateResponse revokeCertificate(UUID certificateId, String reason);
}
