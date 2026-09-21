package com.nqd.nqd_lms_be.controller.student;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.report.ExportCustomizationParams;
import com.nqd.nqd_lms_be.service.report.ReportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
@Tag(name = "Student - Reports & Transcripts", description = "Endpoints for students to export personal transcripts and records")
public class StudentReportController {

    private final ReportExportService reportExportService;

    @GetMapping("/me/transcript.pdf")
    @Operation(summary = "Export student personal academic transcript with course progress, exam scores, certificates and verification QR code to PDF (.pdf)")
    public ResponseEntity<byte[]> exportPersonalTranscriptPdf(
            @RequestParam(required = false) String institutionName,
            @RequestParam(required = false) String reportTitle,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String signerTitle,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        ExportCustomizationParams params = ExportCustomizationParams.builder()
                .institutionName(institutionName)
                .reportTitle(reportTitle)
                .academicYear(academicYear)
                .signerTitle(signerTitle)
                .notes(notes)
                .build();

        byte[] pdfBytes = reportExportService.generateStudentTranscriptPdf(params, principal.getId());

        String filename = "Bang_Diem_Hoc_Tap_Ca_Nhan.pdf";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .body(pdfBytes);
    }
}
