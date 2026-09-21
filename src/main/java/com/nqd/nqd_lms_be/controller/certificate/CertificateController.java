package com.nqd.nqd_lms_be.controller.certificate;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.certificate.CertificateResponse;
import com.nqd.nqd_lms_be.dto.certificate.CertificateVerificationResponse;
import com.nqd.nqd_lms_be.service.certificate.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Certificate Controller", description = "Endpoints for course certificates, public verification and PDF downloads")
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping("/api/v1/certificates/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Get current authenticated student certificates")
    public ResponseEntity<List<CertificateResponse>> getMyCertificates(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        List<CertificateResponse> responses = certificateService.getMyCertificates(principal.getId());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/api/v1/certificates/courses/{courseId}/eligibility")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Check student certificate eligibility for a course")
    public ResponseEntity<com.nqd.nqd_lms_be.dto.certificate.CertificateEligibilityResponse> checkEligibility(
            @PathVariable("courseId") UUID courseId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(certificateService.checkEligibility(principal.getId(), courseId));
    }

    @PostMapping("/api/v1/certificates/courses/{courseId}/claim")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Claim certificate for a course when all requirements are met")
    public ResponseEntity<CertificateResponse> claimCertificate(
            @PathVariable("courseId") UUID courseId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(certificateService.issueCertificate(principal.getId(), courseId));
    }

    @GetMapping("/api/v1/certificates/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Get certificate by ID")
    public ResponseEntity<CertificateResponse> getCertificateById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        boolean isPrivileged = principal != null && (principal.getRoles().contains("ADMIN") || principal.getRoles().contains("TEACHER"));
        UUID userId = principal != null ? principal.getId() : null;
        CertificateResponse response = certificateService.getCertificateById(id, userId, isPrivileged);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/public/certificates/verify/{code}")
    @Operation(summary = "Public endpoint to verify a certificate by code")
    public ResponseEntity<CertificateVerificationResponse> verifyCertificate(@PathVariable("code") String code) {
        CertificateVerificationResponse response = certificateService.verifyCertificate(code);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/certificates/{id}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Download certificate as PDF")
    public ResponseEntity<byte[]> downloadCertificatePdf(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        boolean isPrivileged = principal != null && (principal.getRoles().contains("ADMIN") || principal.getRoles().contains("TEACHER"));
        UUID userId = principal != null ? principal.getId() : null;
        byte[] pdfBytes = certificateService.generateCertificatePdf(id, userId, isPrivileged);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "certificate-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @PostMapping("/api/v1/admin/certificates/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin endpoint to revoke a certificate")
    public ResponseEntity<CertificateResponse> revokeCertificate(
            @PathVariable("id") UUID id,
            @RequestParam(name = "reason", required = false, defaultValue = "Revoked by Administrator") String reason) {
        CertificateResponse response = certificateService.revokeCertificate(id, reason);
        return ResponseEntity.ok(response);
    }
}
