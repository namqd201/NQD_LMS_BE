package com.nqd.nqd_lms_be.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAiAttachmentDto {
    private String fileName;
    private String fileType; // e.g. "image/png", "image/jpeg", "application/pdf", "text/plain"
    private String base64Data; // data:image/png;base64,... or raw base64 string
    private String fileUrl;
    private String extractedText; // Text content extracted from documents/code/text files
}
