package com.nqd.nqd_lms_be.dto.student;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAiTutorConversationRequest {

    @NotBlank(message = "Tiêu đề cuộc trò chuyện không được để trống")
    private String title;

    @Builder.Default
    private StudentAiTutorMode mode = StudentAiTutorMode.GENERAL_QA;

    private UUID courseId;
    private UUID lessonId;
    private UUID examAttemptId;
    private UUID questionId;
}
