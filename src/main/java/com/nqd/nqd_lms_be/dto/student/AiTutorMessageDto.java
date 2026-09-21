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
public class AiTutorMessageDto {
    private UUID id;
    private String role; // "USER", "ASSISTANT", "SYSTEM"
    private String content;
    @Builder.Default
    private List<StudentAiAttachmentDto> attachments = new ArrayList<>();
    @Builder.Default
    private List<StudentStudyRecommendationDto> recommendations = new ArrayList<>();
    private LocalDateTime createdAt;
}
