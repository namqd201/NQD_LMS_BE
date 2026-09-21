package com.nqd.nqd_lms_be.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAiApproveJobResponse {
    private UUID jobId;
    private Integer approvedQuestionsCount;
    private UUID createdExamId;
    private String message;
}
