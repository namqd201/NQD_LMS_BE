package com.nqd.nqd_lms_be.controller.admin;

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
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Reports & Analytics", description = "Endpoints for administrators to export multi-sheet platform overview reports")
public class AdminReportController {

    private final ReportExportService reportExportService;

    @GetMapping("/platform.xlsx")
    @Operation(summary = "Export multi-sheet platform executive analytics to Excel (.xlsx)")
    public ResponseEntity<byte[]> exportPlatformOverviewExcel(
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

        byte[] excelBytes = reportExportService.generateAdminPlatformOverviewExcel(params, principal.getId());

        String filename = "Bao_Cao_Tong_Quan_He_Thong.xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .body(excelBytes);
    }
}
