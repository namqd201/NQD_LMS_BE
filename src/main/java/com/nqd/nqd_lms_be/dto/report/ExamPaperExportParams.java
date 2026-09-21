package com.nqd.nqd_lms_be.dto.report;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamPaperExportParams {

    @Builder.Default
    private String institutionName = "Trường Tiểu học Đồng Tâm";

    private String departmentName;

    private String examTitle;

    @Builder.Default
    private String academicYear = "Năm học: 2024 - 2025";

    private String subjectName;

    private String gradeLevel;

    private Integer durationMinutes;

    @Builder.Default
    private Boolean includeAnswerKey = false;

    @Builder.Default
    private String instructionNote = "Khoanh vào trước câu trả lời đúng nhất.";

    public String getEffectiveInstitutionName() {
        return (institutionName != null && !institutionName.trim().isEmpty())
                ? institutionName.trim()
                : "Trường Tiểu học Đồng Tâm";
    }

    public String getEffectiveExamTitle(String defaultTitle) {
        return (examTitle != null && !examTitle.trim().isEmpty())
                ? examTitle.trim().toUpperCase()
                : (defaultTitle != null ? defaultTitle.trim().toUpperCase() : "ĐỀ KIỂM TRA ĐÁNH GIÁ NĂNG LỰC");
    }

    public String getEffectiveAcademicYear() {
        return (academicYear != null && !academicYear.trim().isEmpty())
                ? academicYear.trim()
                : "Năm học: 2024 - 2025";
    }

    public String getEffectiveInstructionNote() {
        return (instructionNote != null && !instructionNote.trim().isEmpty())
                ? instructionNote.trim()
                : "Khoanh vào trước câu trả lời đúng nhất.";
    }
}
