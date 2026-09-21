package com.nqd.nqd_lms_be.service.moderation;

import com.nqd.nqd_lms_be.ai.GeminiDirectAIProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentModerationServiceTest {

    @Mock
    private GeminiDirectAIProvider geminiDirectAIProvider;

    private ContentModerationServiceImpl moderationService;

    @BeforeEach
    void setUp() {
        moderationService = new ContentModerationServiceImpl(geminiDirectAIProvider);
    }

    @Test
    @DisplayName("Tier 1: Catches vulgar profane keywords immediately via rule-based filter")
    void testTier1CatchesProfanity() {
        ModerationResult result = moderationService.checkContent("Khóa học như lồn, phí tiền");
        assertTrue(result.isViolated());
        assertEquals("PROFANITY", result.getCategory());
        assertNotNull(result.getReason());

        assertThrows(IllegalArgumentException.class, () ->
                moderationService.validateContent("Thằng ngu dạy chán vcl", "Đánh giá")
        );
    }

    @Test
    @DisplayName("Clean review passes through Tier 1 and Tier 2 successfully")
    void testCleanReviewPasses() {
        when(geminiDirectAIProvider.isAvailable()).thenReturn(true);
        when(geminiDirectAIProvider.generateChatResponse(anyString(), anyString(), anyList()))
                .thenReturn("{\"isViolated\": false, \"category\": \"NONE\", \"reason\": \"\"}");

        ModerationResult result = moderationService.checkContent("Bài giảng rất dễ hiểu và hữu ích, cảm ơn thầy cô!");
        assertFalse(result.isViolated());
        assertDoesNotThrow(() -> moderationService.validateContent("Bài giảng rất dễ hiểu và hữu ích, cảm ơn thầy cô!", "Đánh giá"));
    }

    @Test
    @DisplayName("Tier 2: Gemini AI catches complex cyberbullying or toxic speech")
    void testTier2GeminiCatchesToxicity() {
        when(geminiDirectAIProvider.isAvailable()).thenReturn(true);
        when(geminiDirectAIProvider.generateChatResponse(anyString(), anyString(), anyList()))
                .thenReturn("{\"isViolated\": true, \"category\": \"TOXICITY\", \"reason\": \"Nội dung có tính chất xúc phạm bôi nhọ giảng viên\"}");

        ModerationResult result = moderationService.checkContent("Giáo viên này không biết gì hết, lừa đảo mọi người đừng mua");
        assertTrue(result.isViolated());
        assertEquals("TOXICITY", result.getCategory());
        assertEquals("Nội dung có tính chất xúc phạm bôi nhọ giảng viên", result.getReason());

        assertThrows(IllegalArgumentException.class, () ->
                moderationService.validateContent("Giáo viên này không biết gì hết, lừa đảo mọi người đừng mua", "Đánh giá")
        );
    }
}
