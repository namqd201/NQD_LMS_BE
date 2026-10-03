package com.nqd.nqd_lms_be.ai.ocr;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.nqd.nqd_lms_be.ai.GeminiDirectAIProvider;
import com.nqd.nqd_lms_be.dto.student.StudentAiAttachmentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiOcrServiceImpl implements AiOcrService {

    private final GeminiDirectAIProvider geminiDirectAIProvider;

    @Override
    public String extractText(StudentAiAttachmentDto attachment) {
        if (attachment == null) return null;

        // If already has extracted text, return it
        if (attachment.getExtractedText() != null && !attachment.getExtractedText().isBlank()) {
            return attachment.getExtractedText().trim();
        }

        String fileType = attachment.getFileType();
        String fileName = attachment.getFileName();
        String base64Data = attachment.getBase64Data();

        if (base64Data == null || base64Data.isBlank()) {
            return null;
        }

        return extractTextFromBase64(base64Data, fileType, fileName);
    }

    @Override
    public String extractTextFromBase64(String base64Data, String fileType, String fileName) {
        if (base64Data == null || base64Data.isBlank()) {
            return null;
        }

        String raw = base64Data.trim();
        String mimeType = fileType != null ? fileType.toLowerCase(Locale.ROOT) : "image/png";
        String base64Payload = raw;

        if (raw.startsWith("data:")) {
            String[] split = raw.split(",", 2);
            if (split[0].contains(";")) {
                mimeType = split[0].substring(5, split[0].indexOf(";")).toLowerCase(Locale.ROOT);
            }
            base64Payload = split.length > 1 ? split[1] : split[0];
        }

        // Check if file is image
        boolean isImage = mimeType.startsWith("image/") ||
                (fileName != null && (fileName.toLowerCase().endsWith(".png")
                        || fileName.toLowerCase().endsWith(".jpg")
                        || fileName.toLowerCase().endsWith(".jpeg")
                        || fileName.toLowerCase().endsWith(".webp")
                        || fileName.toLowerCase().endsWith(".gif")));

        if (!isImage) {
            return null;
        }

        // Layer 1: Fast local ZXing QR / Barcode detection
        String zxingText = tryDecodeBarcodeOrQr(base64Payload);
        if (zxingText != null && !zxingText.isBlank()) {
            log.info("AiOcrService: Successfully extracted QR/Barcode content from image '{}'", fileName);
            return "[QR/Barcode Content]: " + zxingText.trim();
        }

        // Layer 2: Dedicated High-Precision AI Vision OCR Layer (Gemini Direct)
        if (geminiDirectAIProvider != null && geminiDirectAIProvider.isAvailable()) {
            try {
                log.info("AiOcrService: Performing high-precision multimodal OCR on image '{}'...", fileName);
                String ocrResult = geminiDirectAIProvider.extractTextFromImage(mimeType, base64Payload);
                if (ocrResult != null && !ocrResult.isBlank()) {
                    log.info("AiOcrService: Multimodal OCR successfully extracted {} characters from '{}'", ocrResult.length(), fileName);
                    return ocrResult.trim();
                }
            } catch (Exception e) {
                log.warn("AiOcrService: Vision OCR layer failed for image '{}': {}", fileName, e.getMessage());
            }
        }

        log.info("AiOcrService: No readable text detected in image attachment '{}'", fileName);
        return null;
    }

    private String tryDecodeBarcodeOrQr(String base64Payload) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Payload.replaceAll("\\s+", ""));
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(bais);
            if (image == null) return null;

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            Result result = new MultiFormatReader().decode(bitmap);
            return result != null ? result.getText() : null;
        } catch (NotFoundException ignored) {
            // No barcode or QR code in image, normal case
            return null;
        } catch (Exception e) {
            log.debug("ZXing QR/Barcode decoding skipped or failed: {}", e.getMessage());
            return null;
        }
    }
}
