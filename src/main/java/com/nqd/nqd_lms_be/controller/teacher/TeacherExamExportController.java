package com.nqd.nqd_lms_be.controller.teacher;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.report.ExamPaperExportParams;
import com.nqd.nqd_lms_be.service.report.ExamPaperExportService;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/exams")
@RequiredArgsConstructor
@Tag(name = "Teacher Exam Paper Export", description = "Endpoints for downloading exam papers in Word (.docx) and PDF (.pdf)")
public class TeacherExamExportController {

    private final ExamPaperExportService examPaperExportService;

    @GetMapping("/{examId}/export/docx")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Export exam paper to Word (.docx) with customizable institution and exam title")
    public ResponseEntity<byte[]> exportExamDocx(
            @PathVariable UUID examId,
            @ModelAttribute ExamPaperExportParams params,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        byte[] docxBytes = examPaperExportService.exportExamDocx(examId, params, principal.getId());

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = "De_Thi_" + dateStr + ".docx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(docxBytes);
    }

    @GetMapping("/{examId}/export/pdf")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Export exam paper to PDF (.pdf) with customizable institution and exam title")
    public ResponseEntity<byte[]> exportExamPdf(
            @PathVariable UUID examId,
            @ModelAttribute ExamPaperExportParams params,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        byte[] pdfBytes = examPaperExportService.exportExamPdf(examId, params, principal.getId());

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = "De_Thi_" + dateStr + ".pdf";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
