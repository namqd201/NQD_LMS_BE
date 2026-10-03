package com.nqd.nqd_lms_be.ai.ocr;

import com.nqd.nqd_lms_be.dto.student.StudentAiAttachmentDto;

/**
 * Service for optical character recognition (OCR) and barcode/QR extraction from attachments.
 * Prevents AI hallucinations and ensures accurate reading of math problems, text, and documents.
 */
public interface AiOcrService {

    /**
     * Extracts text from the given attachment if it represents an image.
     *
     * @param attachment StudentAiAttachmentDto with base64Data or fileUrl
     * @return Extracted text string or null / blank if nothing could be read.
     */
    String extractText(StudentAiAttachmentDto attachment);

    /**
     * Extracts text directly from base64 string, mime type, and file name.
     */
    String extractTextFromBase64(String base64Data, String fileType, String fileName);
}
