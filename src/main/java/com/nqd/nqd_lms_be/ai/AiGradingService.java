package com.nqd.nqd_lms_be.ai;

import com.nqd.nqd_lms_be.ai.dto.AiGradingResult;
import com.nqd.nqd_lms_be.entity.Question;
import com.nqd.nqd_lms_be.entity.QuestionOption;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import com.nqd.nqd_lms_be.repository.QuestionOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiGradingService {

    private final QuestionOptionRepository questionOptionRepository;
    private final GeminiDirectAIProvider geminiDirectAIProvider;
    private final AiMediaHelper aiMediaHelper;

    private static final Pattern PREFIX_PATTERN = Pattern.compile(
            "^(đáp án|đáp số|kết quả|kết luận|ans|x|y|z)\\s*[:=]\\s*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    /**
     * Grade student answer for non-choice questions (FILL_IN_THE_BLANK, SHORT_ANSWER, ESSAY)
     * Supports multimodal input: images in teacher's question content and images in student's answer.
     */
    public AiGradingResult grade(Question question, String studentAnswer, BigDecimal maxMarks) {
        BigDecimal targetMaxMarks = maxMarks != null ? maxMarks :
                (question.getDefaultMarks() != null ? question.getDefaultMarks() : BigDecimal.ONE);

        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return AiGradingResult.builder()
                    .isCorrect(false)
                    .marksAwarded(BigDecimal.ZERO)
                    .feedback("Học sinh chưa nhập câu trả lời.")
                    .build();
        }

        String rawStudent = studentAnswer.trim();
        List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(question.getId());
        String expectedAnswer = options.stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                .map(QuestionOption::getOptionText)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");

        QuestionType qType = question.getQuestionType();

        // 1. FAST-PATH: Local normalized comparison for SHORT_ANSWER & FILL_IN_THE_BLANK (when no images involved)
        boolean hasStudentImages = rawStudent.contains("![](") || rawStudent.contains("<img") || rawStudent.contains("/api/v1/public/media/");
        if (!hasStudentImages && (qType == QuestionType.SHORT_ANSWER || qType == QuestionType.FILL_IN_THE_BLANK)) {
            String normStudent = normalizeText(rawStudent);
            String normExpected = normalizeText(expectedAnswer);

            if (!normExpected.isEmpty() && normStudent.equals(normExpected)) {
                return AiGradingResult.builder()
                        .isCorrect(true)
                        .marksAwarded(targetMaxMarks)
                        .feedback("Chính xác! Đáp án hoàn toàn trùng khớp.")
                        .build();
            }

            // Numeric comparison (e.g. 79 vs 79.0 or 3,5 vs 3.5)
            Double studentNum = parseNumeric(normStudent);
            Double expectedNum = parseNumeric(normExpected);
            if (studentNum != null && expectedNum != null) {
                if (Math.abs(studentNum - expectedNum) < 0.0001) {
                    return AiGradingResult.builder()
                            .isCorrect(true)
                            .marksAwarded(targetMaxMarks)
                            .feedback("Chính xác! Kết quả số học hoàn toàn trùng khớp.")
                            .build();
                }
            }
        }

        // Extract any image attachments from question and student answer
        List<AiMediaHelper.ExtractedImage> questionImages = aiMediaHelper.extractImages(question.getContent());
        List<AiMediaHelper.ExtractedImage> studentImages = aiMediaHelper.extractImages(rawStudent);

        String cleanQuestionText = aiMediaHelper.formatTextWithImagePlaceholders(question.getContent());
        String cleanStudentText = aiMediaHelper.formatTextWithImagePlaceholders(rawStudent);

        // 2. AI-PATH: Use Google Gemini Multimodal Vision to semantically evaluate student answer & images
        if (geminiDirectAIProvider.isAvailable()) {
            try {
                AiGradingResult aiResult = geminiDirectAIProvider.gradeAnswerMultimodal(
                        cleanQuestionText,
                        qType,
                        expectedAnswer,
                        question.getExplanation(),
                        cleanStudentText,
                        targetMaxMarks,
                        questionImages,
                        studentImages
                );
                if (aiResult != null) {
                    log.info("AI Multimodal Grading successful for question {}: isCorrect={}, marks={}/{}, questionImages={}, studentImages={}",
                            question.getId(), aiResult.isCorrect(), aiResult.getMarksAwarded(), targetMaxMarks,
                            questionImages.size(), studentImages.size());
                    return aiResult;
                }
            } catch (Exception e) {
                log.warn("Gemini AI grading call failed for question {}: {}", question.getId(), e.getMessage());
            }
        }

        // 3. FALLBACK: Rule-based fallback if AI is unavailable or failed
        if (qType == QuestionType.ESSAY) {
            boolean hasSubstance = !studentImages.isEmpty() || rawStudent.contains("http") || rawStudent.length() >= 10;
            BigDecimal awarded = hasSubstance ? targetMaxMarks : BigDecimal.ZERO;
            String fallbackFeedback = !studentImages.isEmpty()
                    ? "Đã ghi nhận bài làm tự luận có hình ảnh của bạn (Chế độ dự phòng). Giáo viên sẽ chấm lại nếu cần."
                    : (hasSubstance ? "Đã ghi nhận bài làm tự luận của bạn." : "Bài tự luận quá ngắn hoặc chưa đầy đủ.");

            return AiGradingResult.builder()
                    .isCorrect(hasSubstance)
                    .marksAwarded(awarded)
                    .feedback(fallbackFeedback)
                    .build();
        } else {
            // Compare normalized containing
            String normStudent = normalizeText(rawStudent);
            String normExpected = normalizeText(expectedAnswer);
            boolean matched = !normExpected.isEmpty() && (
                    normStudent.contains(normExpected) || normExpected.contains(normStudent)
            );
            return AiGradingResult.builder()
                    .isCorrect(matched)
                    .marksAwarded(matched ? targetMaxMarks : BigDecimal.ZERO)
                    .feedback(matched ? "Đáp án được chấp nhận." : ("Đáp án chưa chính xác. Đáp án tham khảo: " + expectedAnswer))
                    .build();
        }
    }

    private String normalizeText(String input) {
        if (input == null) return "";
        String text = stripHtml(input).trim().toLowerCase();
        text = PREFIX_PATTERN.matcher(text).replaceFirst("").trim();
        // Remove ending punctuation
        text = text.replaceAll("[.,;!?]+$", "").trim();
        return text;
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return HTML_TAG_PATTERN.matcher(html).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    private Double parseNumeric(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            // Replace comma with dot for Vietnamese decimal format
            String sanitized = text.replaceAll("[^0-9.,-]", "").replace(',', '.');
            return Double.parseDouble(sanitized);
        } catch (Exception e) {
            return null;
        }
    }
}
