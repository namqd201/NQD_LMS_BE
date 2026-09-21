package com.nqd.nqd_lms_be.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAiChatMessage {
    private String role; // "USER", "ASSISTANT", "SYSTEM"
    private String content;
    private String timestamp; // String format (e.g. "03:19 PM" or ISO-8601 string)
    @Builder.Default
    private List<StudentAiAttachmentDto> attachments = new ArrayList<>();
}
