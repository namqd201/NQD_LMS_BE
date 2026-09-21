package com.nqd.nqd_lms_be.dto.report;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportCustomizationParams {

    @Builder.Default
    private String institutionName = "HỆ THỐNG GIÁO DỤC TRỰC TUYẾN NQD LMS";

    private String reportTitle;

    @Builder.Default
    private String academicYear = "Năm học 2025 - 2026";

    @Builder.Default
    private String signerTitle = "GIÁO VIÊN PHỤ TRÁCH / BAN GIÁM HIỆU";

    private String notes;

    public String getEffectiveInstitutionName() {
        return (institutionName != null && !institutionName.trim().isEmpty())
                ? institutionName.trim()
                : "HỆ THỐNG GIÁO DỤC TRỰC TUYẾN NQD LMS";
    }

    public String getEffectiveReportTitle(String defaultTitle) {
        return (reportTitle != null && !reportTitle.trim().isEmpty())
                ? reportTitle.trim()
                : defaultTitle;
    }

    public String getEffectiveAcademicYear() {
        return (academicYear != null && !academicYear.trim().isEmpty())
                ? academicYear.trim()
                : "Năm học 2025 - 2026";
    }

    public String getEffectiveSignerTitle() {
        return (signerTitle != null && !signerTitle.trim().isEmpty())
                ? signerTitle.trim()
                : "GIÁO VIÊN PHỤ TRÁCH / BAN GIÁM HIỆU";
    }
}
