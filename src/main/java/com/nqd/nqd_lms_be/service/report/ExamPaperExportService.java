package com.nqd.nqd_lms_be.service.report;

import com.nqd.nqd_lms_be.dto.report.ExamPaperExportParams;

import com.nqd.nqd_lms_be.dto.report.QuestionPaperExportRequest;

import java.util.UUID;

public interface ExamPaperExportService {

    /**
     * Export exam paper in Word (.docx) format matching standard Vietnamese school exam paper.
     */
    byte[] exportExamDocx(UUID examId, ExamPaperExportParams params, UUID teacherId);

    /**
     * Export exam paper in PDF (.pdf) format matching standard Vietnamese school exam paper.
     */
    byte[] exportExamPdf(UUID examId, ExamPaperExportParams params, UUID teacherId);

    /**
     * Export question bank list in Word (.docx) format without answer key.
     */
    byte[] exportQuestionsDocx(QuestionPaperExportRequest request, UUID userId);

    /**
     * Export question bank list in PDF (.pdf) format without answer key.
     */
    byte[] exportQuestionsPdf(QuestionPaperExportRequest request, UUID userId);
}
