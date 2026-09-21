package com.nqd.nqd_lms_be.dto.student;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAiTutorRequest {

    private UUID conversationId;

    @NotBlank(message = "Câu hỏi hoặc yêu cầu học tập không được để trống")
    private String question;

    @Builder.Default
    private StudentAiTutorMode mode = StudentAiTutorMode.GENERAL_QA;

    private UUID courseId;
    private UUID lessonId;
    private UUID examAttemptId;
    private UUID questionId;

    @Builder.Default
    private List<StudentAiAttachmentDto> attachments = new ArrayList<>();

    @Builder.Default
    private List<StudentAiChatMessage> conversationHistory = new ArrayList<>();
}
