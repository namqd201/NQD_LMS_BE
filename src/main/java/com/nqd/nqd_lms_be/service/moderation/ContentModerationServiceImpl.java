package com.nqd.nqd_lms_be.service.moderation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.ai.GeminiDirectAIProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentModerationServiceImpl implements ContentModerationService {

    private final GeminiDirectAIProvider geminiDirectAIProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 1. Blacklist keywords & patterns for Tier 1 Fast Rule-Based Filter
    private static final List<String> BLACKLISTED_TERMS = List.of(
            "địt", "đụ", "lồn", "cặc", "buồi", "đéo", "đél", "đếch", "đĩ", "con đĩ", "đồ chó", "chó đẻ",
            "mẹ kiếp", "đụ má", "đụ mẹ", "đm", "đcm", "dcm", "vcl", "vkl", "clgt", "óc chó", "ngu như chó",
            "đồ ngu", "thằng ngu", "con chó", "chết mẹ", "bố mày", "mẹ mày", "cút đi", "lũ rác rưởi",
            "fuck", "bitch", "asshole", "bastard", "dick", "pussy", "cunt", "shit", "motherfucker"
    );

    private static final List<Pattern> BLACKLISTED_PATTERNS = new ArrayList<>();

    static {
        for (String term : BLACKLISTED_TERMS) {
            // Build regex that allows optional spaces or dots between characters
            StringBuilder patternBuilder = new StringBuilder("\\b");
            for (int i = 0; i < term.length(); i++) {
                char c = term.charAt(i);
                if (c == ' ') {
                    patternBuilder.append("\\s+");
                } else {
                    patternBuilder.append(Pattern.quote(String.valueOf(c)));
                    if (i < term.length() - 1 && term.charAt(i + 1) != ' ') {
                        patternBuilder.append("[.\\-_\\s]*");
                    }
                }
            }
            patternBuilder.append("\\b");
            BLACKLISTED_PATTERNS.add(Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        }
    }

    @Override
    public ModerationResult checkContent(String text) {
        if (text == null || text.isBlank()) {
            return ModerationResult.clean();
        }

        String trimmed = text.trim();

        // --- TIER 1: FAST RULE-BASED CHECK ---
        for (Pattern pattern : BLACKLISTED_PATTERNS) {
            if (pattern.matcher(trimmed).find()) {
                log.warn("Tier 1 Content Moderation triggered for text pattern match: {}", trimmed);
                return ModerationResult.violated(
                        "Nội dung chứa từ ngữ thô tục hoặc không phù hợp với chuẩn mực môi trường giáo dục.",
                        "PROFANITY"
                );
            }
        }

        // --- TIER 2: GEMINI AI CONTENT MODERATION ---
        if (geminiDirectAIProvider != null && geminiDirectAIProvider.isAvailable()) {
            try {
                ModerationResult aiResult = callAiModeration(trimmed);
                if (aiResult != null && aiResult.isViolated()) {
                    log.warn("Tier 2 Gemini AI Content Moderation detected violation: {} | reason: {}", trimmed, aiResult.getReason());
                    return aiResult;
                }
            } catch (Exception e) {
                log.warn("Gemini AI Content Moderation check encountered an error, falling back to rule-based: {}", e.getMessage());
            }
        }

        return ModerationResult.clean();
    }

    @Override
    public void validateContent(String text, String contextName) {
        if (text == null || text.isBlank()) {
            return;
        }

        ModerationResult result = checkContent(text);
        if (result.isViolated()) {
            String prefix = (contextName != null && !contextName.isBlank()) ? contextName : "Nội dung";
            throw new IllegalArgumentException(prefix + " không được chấp nhận do vi phạm tiêu chuẩn cộng đồng: " + result.getReason());
        }
    }

    private ModerationResult callAiModeration(String text) {
        String systemPrompt = "Bạn là Trợ lý Kiểm duyệt Nội dung nghiêm ngặt cho nền tảng giáo dục trực tuyến NQD-LMS.\n" +
                "Nhiệm vụ: Phân tích và phát hiện các vi phạm tiêu chuẩn cộng đồng trong bình luận, đánh giá hoặc câu hỏi của người dùng.\n" +
                "Các hành vi vi phạm bao gồm:\n" +
                "1. Xúc phạm, lăng mạ, bôi nhọ, đe dọa hoặc tấn công cá nhân giáo viên/học sinh.\n" +
                "2. Ngôn từ thô tục, chửi thề trá hình, quấy rối, bắt nạt trực tuyến.\n" +
                "3. Thù ghét, phân biệt đối xử (chủng tộc, giới tính, tôn giáo), bạo lực, khiêu dâm.\n" +
                "4. Lừa đảo, quảng cáo spam đường link độc hại.\n\n" +
                "LƯU Ý QUAN TRỌNG: Những nhận xét phê bình mang tính xây dựng, bày tỏ khóa học khó hiểu, hoặc góp ý chân thành về chất lượng bài giảng thì HOÀN TOÀN HỢP LỆ (KHÔNG vi phạm).\n\n" +
                "BẮT BUỘC trả về duy nhất 1 JSON theo cấu trúc (không kèm giải thích bên ngoài):\n" +
                "{\n" +
                "  \"isViolated\": true hoặc false,\n" +
                "  \"category\": \"TOXICITY / HARASSMENT / PROFANITY / SPAM / NONE\",\n" +
                "  \"reason\": \"Giải thích ngắn gọn 1 câu bằng tiếng Việt lý do vi phạm (nếu không vi phạm thì để chuỗi rỗng)\"\n" +
                "}";

        String userPrompt = "Nội dung cần kiểm duyệt:\n\"\"\"\n" + text + "\n\"\"\"";

        String response = geminiDirectAIProvider.generateChatResponse(systemPrompt, userPrompt, Collections.emptyList());
        if (response == null || response.isBlank()) {
            return null;
        }

        try {
            // Clean up possible markdown code block wrappers
            String jsonContent = response.trim();
            if (jsonContent.startsWith("```json")) {
                jsonContent = jsonContent.substring(7);
            } else if (jsonContent.startsWith("```")) {
                jsonContent = jsonContent.substring(3);
            }
            if (jsonContent.endsWith("```")) {
                jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
            }
            jsonContent = jsonContent.trim();

            JsonNode root = objectMapper.readTree(jsonContent);
            boolean isViolated = root.path("isViolated").asBoolean(false);
            String category = root.path("category").asText("NONE");
            String reason = root.path("reason").asText("Nội dung không phù hợp với tiêu chuẩn cộng đồng");

            if (isViolated) {
                return ModerationResult.violated(reason, category);
            } else {
                return ModerationResult.clean();
            }
        } catch (Exception ex) {
            log.warn("Failed to parse Gemini AI moderation JSON response: {}. Raw was: {}", ex.getMessage(), response);
            return null;
        }
    }
}
