package com.nqd.nqd_lms_be.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class AIProviderFactory {

    private final List<AIProvider> providers;
    private final String configuredProvider;

    public AIProviderFactory(
            List<AIProvider> providers,
            @Value("${lms.ai.provider:auto}") String configuredProvider
    ) {
        this.providers = providers;
        this.configuredProvider = configuredProvider != null ? configuredProvider.trim().toUpperCase() : "AUTO";
    }

    public AIProvider getProvider() {
        if (!"AUTO".equals(configuredProvider)) {
            Optional<AIProvider> matched = providers.stream()
                    .filter(p -> p.getProviderName().equalsIgnoreCase(configuredProvider)
                            || (isLangChain4jAlias(configuredProvider) && "LANGCHAIN4J".equalsIgnoreCase(p.getProviderName()))
                            || ("GEMINI".equalsIgnoreCase(configuredProvider) && "GEMINI".equalsIgnoreCase(p.getProviderName())))
                    .findFirst();

            if (matched.isPresent() && matched.get().isAvailable()) {
                log.info("Using configured AI provider: {}", matched.get().getProviderName());
                return matched.get();
            }
            log.warn("Configured provider '{}' not found or not available. Falling back to auto-selection.", configuredProvider);
        }

        // Auto selection: prefer available LLM provider (OpenAI/Groq or Gemini), otherwise mock fallback
        return providers.stream()
                .filter(p -> !p.getProviderName().equals("MOCK_FALLBACK") && p.isAvailable())
                .findFirst()
                .orElseGet(() -> providers.stream()
                        .filter(p -> p.getProviderName().equals("MOCK_FALLBACK"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No AIProvider registered!")));
    }

    private boolean isLangChain4jAlias(String name) {
        return "OPENAI".equalsIgnoreCase(name) || "GROQ".equalsIgnoreCase(name) 
                || "DEEPSEEK".equalsIgnoreCase(name) || "LANGCHAIN4J".equalsIgnoreCase(name)
                || "OPENROUTER".equalsIgnoreCase(name);
    }
}
