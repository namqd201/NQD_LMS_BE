package com.nqd.nqd_lms_be.ai;

import com.nqd.nqd_lms_be.ai.dto.AiGradingResult;
import com.nqd.nqd_lms_be.entity.Question;
import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionStatus;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import com.nqd.nqd_lms_be.repository.QuestionOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class AiGradingMultimodalTest {

    private AiMediaHelper aiMediaHelper;
    private QuestionOptionRepository questionOptionRepository;
    private GeminiDirectAIProvider geminiDirectAIProvider;
    private AiGradingService aiGradingService;

    @BeforeEach
    void setUp() {
        aiMediaHelper = new AiMediaHelper();
        questionOptionRepository = Mockito.mock(QuestionOptionRepository.class);
        geminiDirectAIProvider = Mockito.mock(GeminiDirectAIProvider.class);
        aiGradingService = new AiGradingService(questionOptionRepository, geminiDirectAIProvider, aiMediaHelper);

        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(any())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("AiMediaHelper extracts images from Data URIs, HTML img tags, and Markdown")
    void testExtractImagesFromText() {
        String testDataUri = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        String content = "<p>Giải bài toán sau:</p>"
                + "<img src=\"" + testDataUri + "\" alt=\"Đề bài 1\" />"
                + "\n![Ảnh bài làm](/api/v1/public/media/nonexistent_sample.png)";

        List<AiMediaHelper.ExtractedImage> images = aiMediaHelper.extractImages(content);
        assertThat(images).isNotEmpty();
        assertThat(images.get(0).getMimeType()).isEqualTo("image/png");
        assertThat(images.get(0).getBase64Data()).isNotBlank();
    }

    @Test
    @DisplayName("AiMediaHelper formats text replacing img tags with clean placeholders")
    void testFormatTextWithImagePlaceholders() {
        String html = "<p>Tính diện tích tam giác:</p><img src=\"/api/v1/public/media/triangle.png\" /><span>Lưu ý đơn vị cm.</span>";
        String formatted = aiMediaHelper.formatTextWithImagePlaceholders(html);

        assertThat(formatted).contains("Tính diện tích tam giác:");
        assertThat(formatted).contains("[Hình ảnh đính kèm]");
        assertThat(formatted).contains("Lưu ý đơn vị cm.");
        assertThat(formatted).doesNotContain("<img");
        assertThat(formatted).doesNotContain("<p>");
    }

    @Test
    @DisplayName("AiGradingService calls multimodal grading when teacher uploads question image and student submits handwritten image")
    void testMultimodalGradingFlow() {
        String testBase64 = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=";

        Question question = Question.builder()
                .content("<p>Xem đề bài trong hình:</p><img src=\"" + testBase64 + "\" />")
                .questionType(QuestionType.ESSAY)
                .difficulty(QuestionDifficulty.MEDIUM)
                .defaultMarks(BigDecimal.valueOf(5.0))
                .status(QuestionStatus.APPROVED)
                .explanation("Lời giải chi tiết từng bước...")
                .build();
        question.setId(UUID.randomUUID());

        String studentAnswer = "Em xin nộp bài làm giải chi tiết trong ảnh:\n<img src=\"" + testBase64 + "\" />";

        when(geminiDirectAIProvider.isAvailable()).thenReturn(true);
        when(geminiDirectAIProvider.gradeAnswerMultimodal(
                anyString(), eq(QuestionType.ESSAY), anyString(), anyString(), anyString(),
                eq(BigDecimal.valueOf(5.0)), anyList(), anyList()
        )).thenReturn(AiGradingResult.builder()
                .isCorrect(true)
                .marksAwarded(BigDecimal.valueOf(4.5))
                .feedback("Học sinh đã giải đúng các bước biến đổi trong ảnh, trừ bước tính nhẩm cuối.")
                .build());

        AiGradingResult result = aiGradingService.grade(question, studentAnswer, BigDecimal.valueOf(5.0));

        assertThat(result).isNotNull();
        assertThat(result.isCorrect()).isTrue();
        assertThat(result.getMarksAwarded()).isEqualByComparingTo("4.5");
        assertThat(result.getFeedback()).contains("Học sinh đã giải đúng các bước biến đổi trong ảnh");
    }
}
