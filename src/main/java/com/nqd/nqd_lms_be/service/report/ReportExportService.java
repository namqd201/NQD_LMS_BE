package com.nqd.nqd_lms_be.service.report;

import com.nqd.nqd_lms_be.dto.report.ExportCustomizationParams;

import java.util.UUID;

public interface ReportExportService {

    /**
     * Export course gradebook to Excel (.xlsx) with student scores, progress, exam breakdown and custom branding.
     */
    byte[] generateTeacherCourseGradebookExcel(UUID courseId, ExportCustomizationParams params, UUID teacherId);

    /**
     * Export official student academic transcript to PDF (.pdf) with course progress, exam grades, certificates and QR code.
     */
    byte[] generateStudentTranscriptPdf(ExportCustomizationParams params, UUID studentId);

    /**
     * Export overall LMS platform executive analytics to multi-sheet Excel (.xlsx) for administrators.
     */
    byte[] generateAdminPlatformOverviewExcel(ExportCustomizationParams params, UUID adminId);
}
