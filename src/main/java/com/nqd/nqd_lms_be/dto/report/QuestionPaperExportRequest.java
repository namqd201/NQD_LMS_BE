package com.nqd.nqd_lms_be.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionPaperExportRequest {

    private List<UUID> questionIds;
    private UUID subjectId;
    private UUID categoryId;
    private String gradeLevel;

    private ExamPaperExportParams params;

    public ExamPaperExportParams getEffectiveParams() {
        if (this.params == null) {
            this.params = new ExamPaperExportParams();
        }
        // Force answer key to false: questions export must never leak answers
        this.params.setIncludeAnswerKey(false);
        return this.params;
    }
}
