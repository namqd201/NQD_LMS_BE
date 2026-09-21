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
import dev.langchain4j.model.openai.OpenAiChatModel;
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
public class LangChain4jAIProvider implements AIProvider {

    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final MockFallbackAIProvider fallbackProvider;
    private final ObjectMapper objectMapper;

    public LangChain4jAIProvider(
            @Value("${lms.ai.langchain4j.api-key:${OPENAI_API_KEY:}}") String apiKey,
            @Value("${lms.ai.langchain4j.model-name:openai/gpt-oss-120b}") String modelName,
            @Value("${lms.ai.langchain4j.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            MockFallbackAIProvider fallbackProvider
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.modelName = modelName != null ? modelName.trim() : "openai/gpt-oss-120b";
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "https://api.groq.com/openai/v1";
        this.fallbackProvider = fallbackProvider;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getProviderName() {
        return "LANGCHAIN4J";
    }

    @Override
    public boolean isAvailable() {
        return !apiKey.isEmpty() && !apiKey.equalsIgnoreCase("demo") && !apiKey.equalsIgnoreCase("mock");
    }

    @Override
    public List<GeneratedQuestionDraft> generateQuestions(AiQuestionGenerationPrompt prompt) {
        if (!isAvailable()) {
            log.info("LangChain4j / Groq API key not configured. Using MockFallbackAIProvider.");
            return fallbackProvider.generateQuestions(prompt);
        }

        List<String> modelsToTry = new ArrayList<>();
        if (modelName != null && !modelName.isBlank()) {
            modelsToTry.add(modelName.trim());
        }
        if (modelName.contains("/")) {
            modelsToTry.add(modelName.substring(modelName.lastIndexOf("/") + 1));
        } else {
            modelsToTry.add("openai/" + modelName);
        }
        if (!modelsToTry.contains("llama-3.3-70b-versatile")) modelsToTry.add("llama-3.3-70b-versatile");
        if (!modelsToTry.contains("llama-3.1-8b-instant")) modelsToTry.add("llama-3.1-8b-instant");

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(prompt);

        for (String currentModel : modelsToTry) {
            try {
                log.info("Calling LangChain4j / Groq endpoint at {} (model: '{}') for question generation...", baseUrl, currentModel);
                ChatLanguageModel chatModel = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(currentModel)
                        .baseUrl(baseUrl)
                        .timeout(Duration.ofSeconds(60))
                        .maxRetries(1)
                        .build();

                String response = chatModel.generate(systemPrompt + "\n\n" + userPrompt);
                log.info("LangChain4j / Groq model '{}' responded successfully. Parsing JSON payload...", currentModel);
                List<GeneratedQuestionDraft> drafts = parseAiResponse(response, prompt);
                if (drafts != null && !drafts.isEmpty()) {
                    return drafts;
                }
            } catch (Exception e) {
                log.warn("Groq / OpenAI model '{}' call failed: {}. Trying next...", currentModel, e.getMessage());
            }
        }

        log.error("All Groq / OpenAI candidate models failed. Falling back to mock generator.");
        return fallbackProvider.generateQuestions(prompt);
    }

    private String buildSystemPrompt() {
        return """
                Bạn là một chuyên gia giáo dục và biên soạn đề thi, câu hỏi trắc nghiệm và tự luận chuẩn theo chương trình học.
                Nhiệm vụ của bạn là tạo ra danh sách câu hỏi có cấu trúc theo yêu cầu.
                YÊU CẦU ĐẦU RA BẮT BUỘC:
                - Định dạng JSON thuần túy, KHÔNG kèm markdown format, KHÔNG kèm lời dẫn.
                - Cấu trúc JSON mẫu:
                {
                  "questions": [
                    {
                      "content": "Nội dung câu hỏi...",
                      "questionType": "MULTIPLE_CHOICE", // MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER, FILL_IN_THE_BLANK, ESSAY
                      "difficulty": "MEDIUM", // EASY, MEDIUM, HARD
                      "defaultMarks": 1.0,
                      "explanation": "Giải thích chi tiết đáp án...",
                      "tags": "toán học, lớp 5",
                      "options": [
                        { "optionKey": "A", "optionText": "Nội dung đáp án A", "isCorrect": true, "displayOrder": 1 },
                        { "optionKey": "B", "optionText": "Nội dung đáp án B", "isCorrect": false, "displayOrder": 2 },
                        { "optionKey": "C", "optionText": "Nội dung đáp án C", "isCorrect": false, "displayOrder": 3 },
                        { "optionKey": "D", "optionText": "Nội dung đáp án D", "isCorrect": false, "displayOrder": 4 }
                      ]
                    }
                  ]
                }
                Quy tắc về loại câu hỏi:
                1. MULTIPLE_CHOICE: Có 4 đáp án (A, B, C, D), đúng 1 đáp án isCorrect = true.
                2. TRUE_FALSE: Có 2 đáp án (A: Đúng, B: Sai), đúng 1 đáp án isCorrect = true.
                3. SHORT_ANSWER / FILL_IN_THE_BLANK: Có 1 option với optionKey = "ANS", optionText là từ/số chính xác, isCorrect = true.
                4. ESSAY: options để trống [].
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
            } else if (prompt.getQuestionType() != null) {
                sb.append("- Loại câu hỏi: ").append(prompt.getQuestionType()).append("\n");
            } else {
                sb.append("- Loại câu hỏi: HỖN HỢP ĐA DẠNG TẤT CẢ CÁC DẠNG (bao gồm cả MULTIPLE_CHOICE trắc nghiệm 4 lựa chọn, TRUE_FALSE đúng/sai, FILL_IN_THE_BLANK điền khuyết, SHORT_ANSWER trả lời ngắn, và ESSAY tự luận). Hãy phân bổ đều số lượng câu hỏi và gán đúng trường 'questionType' tương ứng cho từng câu!\n");
            }
            if (prompt.getDifficulty() != null) sb.append("- Độ khó: ").append(prompt.getDifficulty()).append("\n");
            if (prompt.getMarksPerQuestion() != null) sb.append("- Thang điểm mỗi câu: ").append(prompt.getMarksPerQuestion()).append("\n");
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
            log.warn("Failed to parse JSON response from LangChain4j: {}. Content was: {}", e.getMessage(), rawJson);
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
                    options.add(GeneratedOptionDraft.builder()
                            .optionKey(key != null ? key : String.valueOf((char) ('A' + order - 1)))
                            .optionText(text != null ? text : "")
                            .isCorrect(isCorrect)
                            .displayOrder(order++)
                            .build());
                }
            }
        }

        return GeneratedQuestionDraft.builder()
                .content(content != null ? content : "Câu hỏi chưa có nội dung")
                .questionType(qType)
                .difficulty(diff)
                .defaultMarks(marks)
                .explanation(explanation)
                .tags(tags)
                .options(options)
                .build();
    }
}
