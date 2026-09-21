package com.nqd.nqd_lms_be.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.ai.dto.AiQuestionGenerationPrompt;
import com.nqd.nqd_lms_be.ai.dto.ExamBlueprintItem;
import com.nqd.nqd_lms_be.ai.dto.GeneratedOptionDraft;
import com.nqd.nqd_lms_be.ai.dto.GeneratedQuestionDraft;
import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LangChain4jGeminiAIProvider implements AIProvider {

    private final String apiKey;
    private final String modelName;
    private final MockFallbackAIProvider fallbackProvider;
    private final ObjectMapper objectMapper;

    public LangChain4jGeminiAIProvider(
            @Value("${lms.ai.gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${lms.ai.gemini.model-name:gemini-1.5-flash}") String modelName,
            MockFallbackAIProvider fallbackProvider
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.modelName = modelName;
        this.fallbackProvider = fallbackProvider;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getProviderName() {
        return "LANGCHAIN4J_GEMINI";
    }

    @Override
    public boolean isAvailable() {
        return !apiKey.isEmpty() && !apiKey.equalsIgnoreCase("demo") && !apiKey.equalsIgnoreCase("mock");
    }

    @Override
    public List<GeneratedQuestionDraft> generateQuestions(AiQuestionGenerationPrompt prompt) {
        if (!isAvailable()) {
            log.warn("Gemini API key is not configured or empty. Using MockFallbackAIProvider.");
            return fallbackProvider.generateQuestions(prompt);
        }

        List<String> modelsToTry = new ArrayList<>();
        if (modelName != null && !modelName.isBlank()) {
            modelsToTry.add(modelName.trim());
        }
        if (!modelsToTry.contains("gemini-3.6-flash")) modelsToTry.add("gemini-3.6-flash");
        if (!modelsToTry.contains("gemini-3.7-flash")) modelsToTry.add("gemini-3.7-flash");
        if (!modelsToTry.contains("gemini-3.5-flash-lite")) modelsToTry.add("gemini-3.5-flash-lite");
        if (!modelsToTry.contains("gemini-2.5-flash")) modelsToTry.add("gemini-2.5-flash");
        if (!modelsToTry.contains("gemini-2.0-flash")) modelsToTry.add("gemini-2.0-flash");

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(prompt);
        Exception lastException = null;

        for (String targetModel : modelsToTry) {
            try {
                log.info("Generating questions using LangChain4j Gemini Model '{}'...", targetModel);

                ChatLanguageModel chatModel = GoogleAiGeminiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(targetModel)
                        .temperature(0.7)
                        .timeout(Duration.ofSeconds(60))
                        .build();

                String response = chatModel.generate(systemPrompt + "\n\n" + userPrompt);
                log.info("Gemini model '{}' responded successfully. Parsing JSON payload...", targetModel);

                List<GeneratedQuestionDraft> drafts = parseAiResponse(response, prompt);
                if (drafts != null && !drafts.isEmpty()) {
                    return drafts;
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Gemini model '{}' call failed: {}. Trying next candidate model...", targetModel, e.getMessage());
            }
        }

        log.error("All Gemini candidate models failed. Last error: {}. Falling back to procedural fallback generator.",
                lastException != null ? lastException.getMessage() : "Unknown error", lastException);
        return fallbackProvider.generateQuestions(prompt);
    }

    private String buildSystemPrompt() {
        return """
            Bạn là một trợ lý AI giáo dục chuyên gia trong việc tạo câu hỏi và đề thi chuẩn sư phạm (Việt Nam & quốc tế).
            Quy tắc bắt buộc:
            1. Luôn phản hồi ở định dạng JSON thuần túy (strict JSON). Không thêm văn bản giải thích bên ngoài JSON.
            2. Cấu trúc JSON trả về:
            {
              "questions": [
                {
                  "content": "Nội dung câu hỏi...",
                  "questionType": "MULTIPLE_CHOICE" | "TRUE_FALSE" | "SHORT_ANSWER" | "FILL_IN_THE_BLANK" | "ESSAY",
                  "difficulty": "EASY" | "MEDIUM" | "HARD",
                  "defaultMarks": 1.0,
                  "explanation": "Lời giải thích chi tiết...",
                  "tags": "toan-lop-1, cong-tru",
                  "options": [
                    { "optionKey": "A", "optionText": "Lựa chọn A", "isCorrect": true, "displayOrder": 1 },
                    { "optionKey": "B", "optionText": "Lựa chọn B", "isCorrect": false, "displayOrder": 2 },
                    { "optionKey": "C", "optionText": "Lựa chọn C", "isCorrect": false, "displayOrder": 3 },
                    { "optionKey": "D", "optionText": "Lựa chọn D", "isCorrect": false, "displayOrder": 4 }
                  ]
                }
              ]
            }
            3. Với MULTIPLE_CHOICE: phải có đúng 4 options (A, B, C, D) và duy nhất 1 option có isCorrect = true.
            4. Với TRUE_FALSE: có 2 options (T: Đúng, F: Sai) và duy nhất 1 option có isCorrect = true.
            5. Với SHORT_ANSWER hoặc FILL_IN_THE_BLANK: 1 option với optionKey = "ANS" và optionText là đáp án chính xác.
            6. Nội dung tiếng Việt chuẩn, không sai chính tả, phù hợp đúng độ tuổi / khối lớp học sinh.
            """;
    }

    private String buildUserPrompt(AiQuestionGenerationPrompt prompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hãy tạo các câu hỏi với thông tin sau:\n");
        if (prompt.getSubjectName() != null) sb.append("- Môn học: ").append(prompt.getSubjectName()).append("\n");
        if (prompt.getGradeLevel() != null) sb.append("- Cấp độ/Khối lớp: ").append(prompt.getGradeLevel()).append("\n");
        if (prompt.getCourseName() != null) sb.append("- Khóa học: ").append(prompt.getCourseName()).append("\n");
        if (prompt.getLessonName() != null) sb.append("- Bài học: ").append(prompt.getLessonName()).append("\n");
        if (prompt.getTopic() != null) sb.append("- Chủ đề trọng tâm: ").append(prompt.getTopic()).append("\n");

        if (Boolean.TRUE.equals(prompt.getIsExamBlueprint()) && prompt.getBlueprintItems() != null && !prompt.getBlueprintItems().isEmpty()) {
            sb.append("- Ma trận đề thi (Blueprint):\n");
            for (ExamBlueprintItem bp : prompt.getBlueprintItems()) {
                sb.append(String.format("  + %d câu dạng %s, độ khó %s, chủ đề '%s', điểm mỗi câu: %s\n",
                        bp.getCount(), bp.getQuestionType(), bp.getDifficulty(), bp.getTopic(), bp.getMarksPerQuestion()));
            }
        } else {
            boolean isMixedMode = (prompt.getQuestionType() == null && (prompt.getQuestionTypes() == null || prompt.getQuestionTypes().isEmpty()));
            if (isMixedMode) {
                sb.append("- Chế độ tạo câu hỏi: HỖN HỢP TOÀN DIỆN DO GIÁO VIÊN TỰ ĐỊNH NGHĨA.\n");
                sb.append("  LƯU Ý QUAN TRỌNG NHẤT VỀ SỐ LƯỢNG, DẠNG CÂU VÀ ĐIỂM SỐ:\n");
                sb.append("  Giáo viên đã tự mô tả chi tiết toàn bộ bài tập trong phần 'Ghi chú / Yêu cầu thêm' bên dưới.\n");
                sb.append("  Bạn BẮT BUỘC phải đọc kỹ mô tả đó để tự xác định:\n");
                sb.append("  1. Số lượng câu hỏi cần tạo: hãy tạo CHÍNH XÁC số lượng câu giáo viên yêu cầu trong mô tả (ví dụ nếu giáo viên yêu cầu 3 bài tập thì CHỈ TẠO ĐÚNG 3 CÂU, không tự ý tạo thêm hoặc bớt!).\n");
                sb.append("  2. Dạng câu hỏi (MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER, FILL_IN_THE_BLANK, ESSAY) cho từng câu theo mô tả.\n");
                sb.append("  3. Độ khó riêng cho từng câu (EASY, MEDIUM, HARD) theo mô tả.\n");
                sb.append("  4. Điểm số riêng cho từng câu: bạn PHẢI gán chính xác vào trường 'defaultMarks' của từng câu theo đúng số điểm giáo viên đã yêu cầu (ví dụ: '2 điểm' -> defaultMarks: 2.0; '3 điểm' -> defaultMarks: 3.0; '5 điểm' -> defaultMarks: 5.0).\n");
            } else {
                sb.append("- Số lượng câu hỏi: ").append(prompt.getCount()).append("\n");
                if (prompt.getQuestionTypes() != null && !prompt.getQuestionTypes().isEmpty()) {
                    String typesDesc = prompt.getQuestionTypes().stream()
                            .map(t -> switch (t) {
                                case MULTIPLE_CHOICE -> "MULTIPLE_CHOICE (trắc nghiệm 4 lựa chọn A, B, C, D)";
                                case TRUE_FALSE -> "TRUE_FALSE (Đúng / Sai)";
                                case SHORT_ANSWER -> "SHORT_ANSWER (câu trả lời ngắn)";
                                case FILL_IN_THE_BLANK -> "FILL_IN_THE_BLANK (điền từ vào chỗ trống)";
                                case ESSAY -> "ESSAY (tự luận)";
                            })
                            .collect(Collectors.joining(", "));
                    sb.append("- Loại câu hỏi: HỖN HỢP CÁC DẠNG CÂU HỎI. Hãy tạo đa dạng và xen kẽ các loại sau: ")
                      .append(typesDesc)
                      .append(". Phân bổ đều số lượng câu hỏi giữa các dạng này và nhớ gán trường 'questionType' chính xác tương ứng cho từng câu!\n");
                } else {
                    sb.append("- Loại câu hỏi: ").append(prompt.getQuestionType()).append("\n");
                }
                if (prompt.getDifficulty() != null) sb.append("- Độ khó: ").append(prompt.getDifficulty()).append("\n");
                if (prompt.getMarksPerQuestion() != null) sb.append("- Thang điểm mỗi câu: ").append(prompt.getMarksPerQuestion()).append("\n");
            }
        }

        if (prompt.getAdditionalInstructions() != null && !prompt.getAdditionalInstructions().isBlank()) {
            sb.append("- Ghi chú / Yêu cầu thêm: ").append(prompt.getAdditionalInstructions()).append("\n");
        }

        return sb.toString();
    }

    private List<GeneratedQuestionDraft> parseAiResponse(String rawJson, AiQuestionGenerationPrompt prompt) {
        if (rawJson == null || rawJson.isBlank()) {
            return fallbackProvider.generateQuestions(prompt);
        }

        try {
            String cleanJson = rawJson.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            Map<String, Object> root = objectMapper.readValue(cleanJson, new TypeReference<Map<String, Object>>() {});
            Object questionsObj = root.get("questions");

            if (questionsObj instanceof List<?> list) {
                List<GeneratedQuestionDraft> drafts = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> qMap) {
                        drafts.add(mapToDraft(qMap));
                    }
                }
                if (!drafts.isEmpty()) {
                    return drafts;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse JSON response from LangChain4j Gemini: {}. Content was: {}", e.getMessage(), rawJson);
        }

        return fallbackProvider.generateQuestions(prompt);
    }

    private GeneratedQuestionDraft mapToDraft(Map<?, ?> map) {
        String content = (String) map.get("content");
        String qTypeStr = (String) map.get("questionType");
        String diffStr = (String) map.get("difficulty");
        Object marksObj = map.get("defaultMarks");
        String explanation = (String) map.get("explanation");
        String tags = (String) map.get("tags");

        QuestionType qType = QuestionType.MULTIPLE_CHOICE;
        if (qTypeStr != null) {
            try {
                qType = QuestionType.valueOf(qTypeStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        QuestionDifficulty diff = QuestionDifficulty.MEDIUM;
        if (diffStr != null) {
            try {
                diff = QuestionDifficulty.valueOf(diffStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        BigDecimal marks = BigDecimal.ONE;
        if (marksObj instanceof Number num) {
            marks = BigDecimal.valueOf(num.doubleValue());
        }

        List<GeneratedOptionDraft> options = new ArrayList<>();
        Object optionsObj = map.get("options");
        if (optionsObj instanceof List<?> optList) {
            int order = 1;
            for (Object optItem : optList) {
                if (optItem instanceof Map<?, ?> optMap) {
                    String key = (String) optMap.get("optionKey");
                    String text = (String) optMap.get("optionText");
                    Boolean isCorrect = Boolean.TRUE.equals(optMap.get("isCorrect"));
                    if (key != null && text != null) {
                        options.add(GeneratedOptionDraft.builder()
                                .optionKey(key)
                                .optionText(text)
                                .isCorrect(isCorrect)
                                .displayOrder(order++)
                                .build());
                    }
                }
            }
        }

        return GeneratedQuestionDraft.builder()
                .content(content != null ? content : "Câu hỏi do AI sinh ra")
                .questionType(qType)
                .difficulty(diff)
                .defaultMarks(marks)
                .explanation(explanation)
                .tags(tags)
                .options(options)
                .build();
    }
}
