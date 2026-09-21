package com.nqd.nqd_lms_be.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.ai.dto.AiGradingResult;
import com.nqd.nqd_lms_be.ai.dto.AiQuestionGenerationPrompt;
import com.nqd.nqd_lms_be.ai.dto.ExamBlueprintItem;
import com.nqd.nqd_lms_be.ai.dto.GeneratedOptionDraft;
import com.nqd.nqd_lms_be.ai.dto.GeneratedQuestionDraft;
import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GeminiDirectAIProvider implements AIProvider {

    private final String apiKey;
    private final String defaultModel;
    private final MockFallbackAIProvider fallbackProvider;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiDirectAIProvider(
            @Value("${lms.ai.gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${lms.ai.gemini.model-name:gemini-3.6-flash}") String defaultModel,
            MockFallbackAIProvider fallbackProvider
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.defaultModel = defaultModel != null ? defaultModel.trim() : "gemini-3.6-flash";
        this.fallbackProvider = fallbackProvider;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public String getProviderName() {
        return "GEMINI";
    }

    @Override
    public boolean isAvailable() {
        return !apiKey.isEmpty() && !apiKey.equalsIgnoreCase("demo") && !apiKey.equalsIgnoreCase("mock");
    }

    @Override
    public List<GeneratedQuestionDraft> generateQuestions(AiQuestionGenerationPrompt prompt) {
        if (!isAvailable()) {
            log.info("Gemini API key not configured. Using MockFallbackAIProvider.");
            return fallbackProvider.generateQuestions(prompt);
        }

        List<String> modelsToTry = new ArrayList<>();
        if (defaultModel != null && !defaultModel.isBlank()) {
            modelsToTry.add(defaultModel.trim());
        }
        if (!modelsToTry.contains("gemini-3.6-flash")) modelsToTry.add("gemini-3.6-flash");
        if (!modelsToTry.contains("gemini-3.7-flash")) modelsToTry.add("gemini-3.7-flash");
        if (!modelsToTry.contains("gemini-3.5-flash-lite")) modelsToTry.add("gemini-3.5-flash-lite");
        if (!modelsToTry.contains("gemini-2.5-flash")) modelsToTry.add("gemini-2.5-flash");

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(prompt);

        for (String currentModel : modelsToTry) {
            try {
                log.info("Calling Google Gemini Native Direct API (model: '{}') for question generation...", currentModel);
                String responseJson = callGeminiGenerateContent(currentModel, systemPrompt, userPrompt);
                if (responseJson != null && !responseJson.isBlank()) {
                    String extractedText = extractTextFromGeminiResponse(responseJson);
                    if (extractedText != null && !extractedText.isBlank()) {
                        log.info("Gemini model '{}' generated text response successfully. Parsing question drafts...", currentModel);
                        List<GeneratedQuestionDraft> drafts = parseAiResponse(extractedText, prompt);
                        if (drafts != null && !drafts.isEmpty()) {
                            return drafts;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Gemini direct model '{}' call failed: {}. Trying alternative...", currentModel, e.getMessage());
            }
        }

        log.error("All Gemini direct candidate models failed. Falling back to mock generator.");
        return fallbackProvider.generateQuestions(prompt);
    }

    public String generateChatResponse(String systemPrompt, String userPrompt, List<Map<String, Object>> parts) {
        if (!isAvailable()) return null;

        List<String> modelsToTry = new ArrayList<>();
        if (defaultModel != null && !defaultModel.isBlank()) modelsToTry.add(defaultModel.trim());
        if (!modelsToTry.contains("gemini-3.6-flash")) modelsToTry.add("gemini-3.6-flash");
        if (!modelsToTry.contains("gemini-3.7-flash")) modelsToTry.add("gemini-3.7-flash");
        if (!modelsToTry.contains("gemini-3.5-flash-lite")) modelsToTry.add("gemini-3.5-flash-lite");

        for (String currentModel : modelsToTry) {
            try {
                log.info("Calling Google Gemini Native Direct API (model: '{}') for student chat...", currentModel);
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + currentModel + ":generateContent?key=" + apiKey;

                Map<String, Object> requestPayload = new HashMap<>();
                if (systemPrompt != null && !systemPrompt.isBlank()) {
                    requestPayload.put("system_instruction", Map.of(
                            "parts", List.of(Map.of("text", systemPrompt))
                    ));
                }

                List<Map<String, Object>> contentsParts = new ArrayList<>();
                if (parts != null && !parts.isEmpty()) {
                    contentsParts.addAll(parts);
                }
                if (userPrompt != null && !userPrompt.isBlank()) {
                    contentsParts.add(Map.of("text", userPrompt));
                }

                requestPayload.put("contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", contentsParts
                        )
                ));

                String jsonBody = objectMapper.writeValueAsString(requestPayload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String extracted = extractTextFromGeminiResponse(response.body());
                    if (extracted != null && !extracted.isBlank()) {
                        return extracted.trim();
                    }
                } else {
                    log.warn("Gemini model '{}' HTTP {} error: {}", currentModel, response.statusCode(), response.body());
                }
            } catch (Exception e) {
                log.warn("Gemini direct chat call for model '{}' failed: {}", currentModel, e.getMessage());
            }
        }
        return null;
    }

    public AiGradingResult gradeAnswer(
            String questionContent,
            QuestionType questionType,
            String expectedAnswer,
            String explanation,
            String studentAnswer,
            BigDecimal maxMarks
    ) {
        return gradeAnswerMultimodal(
                questionContent, questionType, expectedAnswer, explanation, studentAnswer, maxMarks,
                Collections.emptyList(), Collections.emptyList()
        );
    }

    public AiGradingResult gradeAnswerMultimodal(
            String questionContent,
            QuestionType questionType,
            String expectedAnswer,
            String explanation,
            String studentAnswer,
            BigDecimal maxMarks,
            List<AiMediaHelper.ExtractedImage> questionImages,
            List<AiMediaHelper.ExtractedImage> studentImages
    ) {
        if (!isAvailable()) return null;

        List<String> modelsToTry = new ArrayList<>();
        if (defaultModel != null && !defaultModel.isBlank()) modelsToTry.add(defaultModel.trim());
        if (!modelsToTry.contains("gemini-3.6-flash")) modelsToTry.add("gemini-3.6-flash");
        if (!modelsToTry.contains("gemini-3.7-flash")) modelsToTry.add("gemini-3.7-flash");
        if (!modelsToTry.contains("gemini-3.5-flash-lite")) modelsToTry.add("gemini-3.5-flash-lite");

        boolean hasImages = (questionImages != null && !questionImages.isEmpty()) ||
                            (studentImages != null && !studentImages.isEmpty());

        String systemPrompt = """
                Bạn là giám khảo chấm thi AI công bằng, thông minh và chuẩn xác theo chuẩn giáo dục Việt Nam, có năng lực thị giác máy tính Multimodal (Vision / OCR).
                Nhiệm vụ của bạn là đánh giá bài làm của học sinh so với câu hỏi và đáp án mẫu/hướng dẫn giải, BAO GỒM CẢ CÁC HÌNH ẢNH ĐÍNH KÈM (ẢNH ĐỀ BÀI CỦA GIÁO VIÊN HOẶC ẢNH CHỤP BÀI LÀM VIẾT TAY CỦA HỌC SINH).

                YÊU CẦU ĐẦU RA BẮT BUỘC:
                - Trả về JSON thuần túy, KHÔNG dùng markdown format (không bọc ```json), KHÔNG kèm giải thích bên ngoài JSON.
                - Cấu trúc JSON:
                {
                  "isCorrect": true,
                  "marksAwarded": 1.0,
                  "feedback": "Nhận xét ngắn gọn chỉ rõ điểm đúng hoặc lỗi sai nếu có, đối chiếu cụ thể các bước trong đề bài và bài làm."
                }

                QUY TẮC ĐÁNH GIÁ:
                1. NẾU ĐỀ BÀI CÓ HÌNH ẢNH (ẢNH ĐỀ BÀI DO GIÁO VIÊN ĐĂNG):
                   - Hãy dùng OCR đọc kỹ đề bài trong ảnh: câu hỏi, công thức toán học, sơ đồ hình học, bảng biểu hoặc đồ thị.
                   - Hiểu rõ yêu cầu bài toán từ hình ảnh đề bài kết hợp cùng phần chữ mô tả.
                2. NẾU BÀI LÀM HỌC SINH CÓ HÌNH ẢNH (ẢNH CHỤP VỞ / BÀI LÀM VIẾT TAY):
                   - Hãy dùng năng lực OCR chữ viết tay để đọc bài giải của học sinh trên từng trang ảnh đính kèm.
                   - Phân tích chi tiết từng bước giải: các bước biến đổi, tính toán, kết luận.
                   - So sánh với đáp án chuẩn và thang điểm barem để cho điểm thành phần công bằng (marksAwarded từ 0.00 đến maxMarks).
                   - Chỉ rõ trong feedback: học sinh làm đúng đến bước nào, bị sai ở bước nào (ví dụ: sai dấu, sai tính toán, hoặc thiếu kết luận).
                3. Dạng câu hỏi SHORT_ANSWER hoặc FILL_IN_THE_BLANK:
                   - Đánh giá ngữ nghĩa toán học, khoa học, ngôn ngữ.
                   - Chấp nhận các biến thể tương đương: ví dụ học sinh gõ "x = 79", "79", "79.0", "79 viên bi", "79 viên", "79 cm" khi đáp số là 79 -> HỌC SINH ĐÚNG HOÀN TOÀN, marksAwarded = điểm tối đa, isCorrect = true.
                   - Bỏ qua sai khác về viết hoa/thường, khoảng trắng, các tiền tố dẫn nhập như "kết quả là", "đáp số:".
                   - Nếu sai đáp số hoặc sai bản chất -> isCorrect = false, marksAwarded = 0.
                4. Dạng câu hỏi ESSAY (Tự luận):
                   - Đánh giá lập luận, các bước giải và kết quả theo barem điểm tối đa.
                   - Cho điểm thành phần (marksAwarded từ 0.00 đến maxMarks).
                   - isCorrect = true nếu học sinh đạt từ 50% điểm tối đa trở lên, false nếu dưới 50% hoặc lạc đề.
                """;

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Thông tin câu hỏi và bài làm của học sinh:\n");
        userPrompt.append("- Dạng câu hỏi: ").append(questionType != null ? questionType.name() : "UNKNOWN").append("\n");
        userPrompt.append("- Đề bài: ").append(questionContent != null && !questionContent.isBlank() ? questionContent : "(Đề bài được cung cấp trực tiếp qua hình ảnh đính kèm)").append("\n");
        if (expectedAnswer != null && !expectedAnswer.isBlank()) {
            userPrompt.append("- Đáp án chuẩn / Đáp số: ").append(expectedAnswer).append("\n");
        }
        if (explanation != null && !explanation.isBlank()) {
            userPrompt.append("- Hướng dẫn giải / Giải thích: ").append(explanation).append("\n");
        }
        userPrompt.append("- Điểm tối đa: ").append(maxMarks != null ? maxMarks : BigDecimal.ONE).append("\n");
        userPrompt.append("- Bài làm của học sinh: ").append(studentAnswer != null && !studentAnswer.isBlank() ? studentAnswer : (studentImages != null && !studentImages.isEmpty() ? "(Học sinh nộp bài giải bằng hình ảnh đính kèm)" : "")).append("\n");

        List<Map<String, Object>> requestParts = new ArrayList<>();

        // 1. Attach Teacher Question Images if any
        if (questionImages != null && !questionImages.isEmpty()) {
            requestParts.add(Map.of("text", "=== HÌNH ẢNH ĐỀ BÀI CỦA GIÁO VIÊN (HÃY ĐỌC VÀ HIỂU ĐỀ TRONG ẢNH): ==="));
            for (AiMediaHelper.ExtractedImage img : questionImages) {
                requestParts.add(img.toGeminiPart());
            }
        }

        // 2. Attach Student Answer Images if any
        if (studentImages != null && !studentImages.isEmpty()) {
            requestParts.add(Map.of("text", "=== HÌNH ẢNH BÀI LÀM VIẾT TAY CỦA HỌC SINH (HÃY OCR VÀ CHẤM TỪNG BƯỚC): ==="));
            for (AiMediaHelper.ExtractedImage img : studentImages) {
                requestParts.add(img.toGeminiPart());
            }
        }

        // 3. Attach Text Instruction Prompt
        requestParts.add(Map.of("text", userPrompt.toString()));

        for (String currentModel : modelsToTry) {
            try {
                log.info("Calling Gemini model '{}' for AI {} auto-grading...", currentModel, hasImages ? "MULTIMODAL VISION" : "TEXT");
                String responseJson = callGeminiGenerateContent(currentModel, systemPrompt, requestParts);
                if (responseJson != null && !responseJson.isBlank()) {
                    String extracted = extractTextFromGeminiResponse(responseJson);
                    if (extracted != null && !extracted.isBlank()) {
                        AiGradingResult result = parseGradingResponse(extracted, maxMarks);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Gemini grading failed with model '{}': {}", currentModel, e.getMessage());
            }
        }
        return null;
    }

    private AiGradingResult parseGradingResponse(String rawJson, BigDecimal maxMarks) {
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
            Boolean isCorrect = (Boolean) root.get("isCorrect");
            Object marksObj = root.get("marksAwarded");
            String feedback = (String) root.get("feedback");

            BigDecimal marksAwarded = BigDecimal.ZERO;
            if (marksObj instanceof Number num) {
                marksAwarded = BigDecimal.valueOf(num.doubleValue()).setScale(2, RoundingMode.HALF_UP);
            } else if (marksObj instanceof String str) {
                try {
                    marksAwarded = new BigDecimal(str.trim()).setScale(2, RoundingMode.HALF_UP);
                } catch (Exception ignored) {}
            }

            BigDecimal effectiveMax = maxMarks != null ? maxMarks : BigDecimal.ONE;
            if (marksAwarded.compareTo(effectiveMax) > 0) {
                marksAwarded = effectiveMax;
            }

            boolean correct = Boolean.TRUE.equals(isCorrect) || (marksAwarded.compareTo(effectiveMax.multiply(BigDecimal.valueOf(0.5))) >= 0);

            return AiGradingResult.builder()
                    .isCorrect(correct)
                    .marksAwarded(marksAwarded)
                    .feedback(feedback != null ? feedback.trim() : "")
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse AI grading response: {}. Raw was: {}", e.getMessage(), rawJson);
        }
        return null;
    }

    private String callGeminiGenerateContent(String model, String systemPrompt, String userPrompt) throws Exception {
        return callGeminiGenerateContent(model, systemPrompt, List.of(Map.of("text", userPrompt)));
    }

    private String callGeminiGenerateContent(String model, String systemPrompt, List<Map<String, Object>> parts) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> requestPayload = new HashMap<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            requestPayload.put("system_instruction", Map.of(
                    "parts", List.of(Map.of("text", systemPrompt))
            ));
        }

        requestPayload.put("contents", List.of(
                Map.of(
                        "role", "user",
                        "parts", parts
                )
        ));

        String jsonBody = objectMapper.writeValueAsString(requestPayload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(45))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            log.warn("Gemini generateContent returned HTTP status {}: {}", response.statusCode(), response.body());
            return null;
        }
    }

    private String extractTextFromGeminiResponse(String responseJson) {
        try {
            Map<String, Object> root = objectMapper.readValue(responseJson, new TypeReference<Map<String, Object>>() {});
            Object candidatesObj = root.get("candidates");
            if (candidatesObj instanceof List<?> candidates && !candidates.isEmpty()) {
                Object firstCand = candidates.get(0);
                if (firstCand instanceof Map<?, ?> candMap) {
                    Object contentObj = candMap.get("content");
                    if (contentObj instanceof Map<?, ?> contentMap) {
                        Object partsObj = contentMap.get("parts");
                        if (partsObj instanceof List<?> partsList) {
                            StringBuilder sb = new StringBuilder();
                            for (Object p : partsList) {
                                if (p instanceof Map<?, ?> pMap && pMap.get("text") != null) {
                                    sb.append(pMap.get("text")).append("\n");
                                }
                            }
                            return sb.toString().trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing Gemini response JSON: {}", e.getMessage());
        }
        return null;
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
            log.warn("Failed to parse JSON response from Gemini direct: {}. Content was: {}", e.getMessage(), rawJson);
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
        } else if (marksObj instanceof String str) {
            try {
                marks = new BigDecimal(str.trim());
            } catch (Exception ignored) {}
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
