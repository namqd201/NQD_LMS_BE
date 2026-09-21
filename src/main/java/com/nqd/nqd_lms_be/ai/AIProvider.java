package com.nqd.nqd_lms_be.ai;

import com.nqd.nqd_lms_be.ai.dto.AiQuestionGenerationPrompt;
import com.nqd.nqd_lms_be.ai.dto.GeneratedQuestionDraft;

import java.util.List;

public interface AIProvider {
    /**
     * Unique identifier/name of the AI provider (e.g. "LANGCHAIN4J_OPENAI", "LANGCHAIN4J_GEMINI", "MOCK_FALLBACK")
     */
    String getProviderName();

    /**
     * Whether this provider is configured and available for requests
     */
    boolean isAvailable();

    /**
     * Generate structured question drafts based on teacher prompt / blueprint
     */
    List<GeneratedQuestionDraft> generateQuestions(AiQuestionGenerationPrompt prompt);
}
