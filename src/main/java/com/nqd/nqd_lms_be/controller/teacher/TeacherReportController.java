package com.nqd.nqd_lms_be.controller.teacher;

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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/courses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher - Reports & Gradebooks", description = "Endpoints for teachers to export course gradebooks and analytics")
public class TeacherReportController {

    private final ReportExportService reportExportService;

    @GetMapping("/{courseId}/report.xlsx")
    @Operation(summary = "Export course gradebook with student progress, exam scores and customizable institution branding to Excel (.xlsx)")
    public ResponseEntity<byte[]> exportCourseGradebookExcel(
            @PathVariable UUID courseId,
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

        byte[] excelBytes = reportExportService.generateTeacherCourseGradebookExcel(courseId, params, principal.getId());

        String filename = "Bang_Diem_Lop_Hoc_" + courseId.toString().substring(0, 8) + ".xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .body(excelBytes);
    }
}
