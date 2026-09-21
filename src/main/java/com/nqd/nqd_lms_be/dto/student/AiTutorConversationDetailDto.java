package com.nqd.nqd_lms_be.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTutorConversationDetailDto {
    private UUID id;
    private String title;
    private StudentAiTutorMode mode;
    private UUID courseId;
    private UUID lessonId;
    private UUID examAttemptId;
    private UUID questionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Builder.Default
    private List<AiTutorMessageDto> messages = new ArrayList<>();
}
