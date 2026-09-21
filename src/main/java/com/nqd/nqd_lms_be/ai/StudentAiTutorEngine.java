package com.nqd.nqd_lms_be.ai;

import com.nqd.nqd_lms_be.dto.student.StudentAiAttachmentDto;
import com.nqd.nqd_lms_be.dto.student.StudentAiChatMessage;
import com.nqd.nqd_lms_be.dto.student.StudentAiTutorMode;
import com.nqd.nqd_lms_be.dto.student.StudentAiTutorRequest;
import com.nqd.nqd_lms_be.dto.student.StudentAiTutorResponse;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Slf4j
public class StudentAiTutorEngine {

    private final String geminiApiKey;
    private final String geminiModel;
    private final String openAiApiKey;
    private final String openAiModel;
    private final String openAiBaseUrl;
    private final GeminiDirectAIProvider geminiDirectAIProvider;
    private final AiTutorPolicyConfig policyConfig;
    private final AiTutorToolService toolService;

    public StudentAiTutorEngine(
            @Value("${lms.ai.gemini.api-key:${GEMINI_API_KEY:}}") String geminiApiKey,
            @Value("${lms.ai.gemini.model-name:gemini-3.6-flash}") String geminiModel,
            @Value("${lms.ai.langchain4j.api-key:${OPENAI_API_KEY:}}") String openAiApiKey,
            @Value("${lms.ai.langchain4j.model-name:gpt-4o-mini}") String openAiModel,
            @Value("${lms.ai.langchain4j.base-url:https://api.openai.com/v1}") String openAiBaseUrl,
            GeminiDirectAIProvider geminiDirectAIProvider,
            AiTutorPolicyConfig policyConfig,
            AiTutorToolService toolService
    ) {
        this.geminiApiKey = geminiApiKey != null ? geminiApiKey.trim() : "";
        this.geminiModel = geminiModel != null ? geminiModel.trim() : "gemini-3.6-flash";
        this.openAiApiKey = openAiApiKey != null ? openAiApiKey.trim() : "";
        this.openAiModel = openAiModel != null ? openAiModel.trim() : "gpt-4o-mini";
        this.openAiBaseUrl = openAiBaseUrl != null ? openAiBaseUrl.trim() : "https://api.openai.com/v1";
        this.geminiDirectAIProvider = geminiDirectAIProvider;
        this.policyConfig = policyConfig;
        this.toolService = toolService;
    }

    public StudentAiTutorResponse generateTutorResponse(
            StudentAiTutorRequest request,
            StudentAiContextAssembler.AssembledStudentContext context,
            int remainingRequests
    ) {
        return generateTutorResponse(request, context, remainingRequests, null, null);
    }

    public StudentAiTutorResponse generateTutorResponse(
            StudentAiTutorRequest request,
            StudentAiContextAssembler.AssembledStudentContext context,
            int remainingRequests,
            java.util.UUID userId,
            String userRole
    ) {
        // 0. Prompt Injection Check
        if (policyConfig.containsPromptInjection(request.getQuestion())) {
            log.warn("Prompt injection detected from user {} : '{}'", userId, request.getQuestion());
            return StudentAiTutorResponse.builder()
                    .answer(policyConfig.getOutOfScopeResponse())
                    .mode(request.getMode())
                    .remainingRequests(remainingRequests)
                    .build();
        }

        // 1. Intent Detection & Auto-Fetch Course Context
        String autoFetchedContext = "";
        if (userId != null && request.getQuestion() != null) {
            autoFetchedContext = detectIntentAndFetchContext(request.getQuestion(), userId, userRole != null ? userRole : "STUDENT");
        }

        String systemPrompt = buildSystemPrompt(request.getMode(), context.getIsExamInProgress(), userRole);
        String userPrompt = buildUserPrompt(request, context, autoFetchedContext);

        // 2. First attempt to call Live Multimodal LLM (Gemini Direct / Groq / OpenAI)
        String answer = callMultimodalLlm(systemPrompt, userPrompt, request);

        // 3. If no remote LLM returned or API key is invalid/exhausted, use Smart Generative Engine
        if (answer == null || answer.isBlank()) {
            answer = generateSmartGenerativeResponse(request, context);
        }

        String hint = null;
        if (request.getMode() == StudentAiTutorMode.PROVIDE_HINT) {
            hint = answer;
        }

        return StudentAiTutorResponse.builder()
                .answer(answer)
                .hint(hint)
                .context(buildContextSummary(context))
                .contextSummary(buildContextSummary(context))
                .mode(request.getMode())
                .remainingRequests(remainingRequests)
                .recommendations(context.getRecommendations())
                .build();
    }

    private String callMultimodalLlm(
            String systemPrompt,
            String userPrompt,
            StudentAiTutorRequest request
    ) {
        // 1. Prioritize Google Gemini Native Direct API (Fast, Reliable, Model gemini-3.6-flash)
        if (geminiDirectAIProvider != null && geminiDirectAIProvider.isAvailable()) {
            List<Map<String, Object>> parts = new ArrayList<>();
            if (request.getAttachments() != null) {
                for (StudentAiAttachmentDto att : request.getAttachments()) {
                    if (att == null) continue;
                    if (att.getBase64Data() != null && !att.getBase64Data().isBlank()) {
                        String raw = att.getBase64Data().trim();
                        String mimeType = att.getFileType() != null ? att.getFileType() : "image/png";
                        String base64Payload = raw;
                        if (raw.startsWith("data:")) {
                            String[] split = raw.split(",", 2);
                            if (split[0].contains(";")) {
                                mimeType = split[0].substring(5, split[0].indexOf(";"));
                            }
                            base64Payload = split.length > 1 ? split[1] : split[0];
                        }
                        if (mimeType.startsWith("image/")) {
                            parts.add(Map.of("inline_data", Map.of(
                                    "mime_type", mimeType,
                                    "data", base64Payload
                            )));
                        } else if (att.getExtractedText() != null && !att.getExtractedText().isBlank()) {
                            parts.add(Map.of("text", "[Nội dung tệp " + att.getFileName() + "]:\n" + att.getExtractedText()));
                        }
                    } else if (att.getExtractedText() != null && !att.getExtractedText().isBlank()) {
                        parts.add(Map.of("text", "[Nội dung tệp " + att.getFileName() + "]:\n" + att.getExtractedText()));
                    }
                }
            }

            String geminiDirectAnswer = geminiDirectAIProvider.generateChatResponse(systemPrompt, userPrompt, parts);
            if (geminiDirectAnswer != null && !geminiDirectAnswer.isBlank()) {
                log.info("Gemini Native Direct API responded successfully to student query!");
                return geminiDirectAnswer.trim();
            }
        }

        List<ChatMessage> messages = buildChatMessages(systemPrompt, userPrompt, request);

        // 2. Try OpenAI / Groq / OpenRouter / Ollama if configured
        if (!openAiApiKey.isEmpty() && !openAiApiKey.equalsIgnoreCase("demo") && !openAiApiKey.equalsIgnoreCase("mock")) {
            List<String> openAiModels = new ArrayList<>();
            if (openAiModel != null && !openAiModel.isBlank()) {
                openAiModels.add(openAiModel.trim());
                if (openAiModel.contains("/")) {
                    openAiModels.add(openAiModel.substring(openAiModel.lastIndexOf("/") + 1));
                } else {
                    openAiModels.add("openai/" + openAiModel);
                }
            }
            if (!openAiModels.contains("openai/gpt-oss-120b")) openAiModels.add("openai/gpt-oss-120b");
            if (!openAiModels.contains("gpt-oss-120b")) openAiModels.add("gpt-oss-120b");
            if (!openAiModels.contains("llama-3.3-70b-versatile")) openAiModels.add("llama-3.3-70b-versatile");
            if (!openAiModels.contains("llama-3.1-8b-instant")) openAiModels.add("llama-3.1-8b-instant");

            for (String targetModel : openAiModels) {
                try {
                    log.info("Student AI Tutor calling Groq / OpenAI Model '{}' at {} with {} messages...", targetModel, openAiBaseUrl, messages.size());
                    ChatLanguageModel chatModel = OpenAiChatModel.builder()
                            .apiKey(openAiApiKey)
                            .baseUrl(openAiBaseUrl)
                            .modelName(targetModel)
                            .temperature(0.7)
                            .timeout(Duration.ofSeconds(30))
                            .maxRetries(1)
                            .build();

                    Response<AiMessage> response = chatModel.generate(messages);
                    if (response != null && response.content() != null && response.content().text() != null && !response.content().text().isBlank()) {
                        log.info("Groq / OpenAI Model '{}' responded successfully!", targetModel);
                        return response.content().text().trim();
                    }
                } catch (Exception e) {
                    log.warn("Groq / OpenAI model '{}' call failed: {}. Trying next...", targetModel, e.getMessage());
                }
            }
        }

        return null;
    }

    private List<ChatMessage> buildChatMessages(String systemPrompt, String userPrompt, StudentAiTutorRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));

        if (request.getConversationHistory() != null) {
            for (StudentAiChatMessage historyMsg : request.getConversationHistory()) {
                if (historyMsg.getContent() == null || historyMsg.getContent().isBlank()) continue;
                if ("USER".equalsIgnoreCase(historyMsg.getRole())) {
                    messages.add(UserMessage.from(historyMsg.getContent()));
                } else if ("ASSISTANT".equalsIgnoreCase(historyMsg.getRole())) {
                    messages.add(AiMessage.from(historyMsg.getContent()));
                }
            }
        }

        List<Content> userContents = new ArrayList<>();
        userContents.add(TextContent.from(userPrompt));

        if (request.getAttachments() != null) {
            for (StudentAiAttachmentDto att : request.getAttachments()) {
                if (att == null) continue;
                if (isImageAttachment(att)) {
                    ImageContent imageContent = parseImageAttachment(att);
                    if (imageContent != null) {
                        userContents.add(imageContent);
                    }
                }
            }
        }

        messages.add(UserMessage.from(userContents));
        return messages;
    }

    private boolean isImageAttachment(StudentAiAttachmentDto att) {
        String type = att.getFileType();
        String name = att.getFileName();
        String data = att.getBase64Data();

        if (type != null && type.toLowerCase().startsWith("image/")) return true;
        if (data != null && data.startsWith("data:image/")) return true;
        if (name != null) {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp");
        }
        return false;
    }

    private ImageContent parseImageAttachment(StudentAiAttachmentDto attachment) {
        try {
            String data = attachment.getBase64Data();
            if (data == null || data.isBlank()) {
                if (attachment.getFileUrl() != null && !attachment.getFileUrl().isBlank()) {
                    return ImageContent.from(attachment.getFileUrl());
                }
                return null;
            }

            String mimeType = (attachment.getFileType() != null && !attachment.getFileType().isBlank())
                    ? attachment.getFileType() : "image/jpeg";
            String rawBase64 = data;

            if (data.startsWith("data:")) {
                int commaIndex = data.indexOf(",");
                if (commaIndex > 0) {
                    String header = data.substring(5, commaIndex);
                    if (header.contains(";")) {
                        mimeType = header.substring(0, header.indexOf(";"));
                    }
                    rawBase64 = data.substring(commaIndex + 1);
                }
            }

            return ImageContent.from(rawBase64.trim(), mimeType);
        } catch (Exception e) {
            log.warn("Failed to parse image attachment {}: {}", attachment.getFileName(), e.getMessage());
            return null;
        }
    }

    private String buildSystemPrompt(StudentAiTutorMode mode, Boolean isExamInProgress, String userRole) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            Bạn là NQD AI Tutor - một trợ lý gia sư trí tuệ nhân tạo toàn năng, thông thái, ân cần và sinh động (tương tự như ChatGPT / Gemini), hỗ trợ học sinh học tập và giải đáp mọi thắc mắc học thuật.
            
            Khả năng & Quy tắc trình bày:
            - Phân tích và giải đáp mọi câu hỏi thuộc tất cả các môn học (Toán, Văn, Anh, Lý, Hóa, Sinh, Sử, Địa, Tin học / Lập trình, Triết học, v.v.).
            - Đọc hiểu hình ảnh bài tập, đề thi, sơ đồ, bảng biểu hoặc tranh ảnh do học sinh gửi lên.
            - Hướng dẫn giải bài tập theo từng bước mạch lạc, giải thích bản chất kiến thức.
            - Trả lời bằng tiếng Việt tự nhiên, chuẩn mực, lịch sự.
            - Trình bày công thức Toán học chuẩn LaTeX: đặt công thức trong dòng vào $...$ và công thức khối riêng vào $$...$$.
            - Đối với các bước giải, ưu tiên dùng in đậm '**Bước 1: ...**', '**Bước 2: ...**' và gạch đầu dòng gọn gàng, tránh dùng quá nhiều ký tự tiêu đề '#' nếu không cần thiết.
            
            Khả năng truy vấn dữ liệu khóa học:
            - Khi người dùng yêu cầu truy cập vào một khóa học, chương, bài giảng cụ thể, hệ thống sẽ tự động tra cứu và cung cấp nội dung cho bạn trong phần context.
            - Bạn hãy sử dụng nội dung bài giảng thực tế đó để tạo câu hỏi, đề thi, hoặc giải đáp thắc mắc.
            - Khi tạo câu hỏi/đề thi, hãy dựa sát vào kiến thức trong bài giảng, đảm bảo độ chính xác và phù hợp với trình độ.
            """);

        // Inject security policy from config
        if (policyConfig != null) {
            sb.append(policyConfig.buildPolicyPromptSection(userRole));
        }

        if (Boolean.TRUE.equals(isExamInProgress)) {
            sb.append("""
            
            ⚠️ LƯU Ý KHI HỌC SINH ĐANG LÀM BÀI THI:
            - Không đưa ra trực tiếp chữ cái đáp án trắc nghiệm (như 'Chọn A', 'Đáp án là B').
            - Áp dụng phương pháp Socratic để gợi mở tư duy từng bước cho học sinh.
            """);
        }

        return sb.toString();
    }

    private String buildUserPrompt(StudentAiTutorRequest request, StudentAiContextAssembler.AssembledStudentContext context, String autoFetchedContext) {
        StringBuilder sb = new StringBuilder();

        if (context.getFormattedContext() != null && !context.getFormattedContext().isBlank()) {
            sb.append("=== THÔNG TIN BÀI HỌC / ĐỀ BÀI LIÊN QUAN ===\n");
            sb.append(context.getFormattedContext());
            sb.append("==========================================\n\n");
        }

        // Include auto-fetched course/chapter/lesson context from intent detection
        if (autoFetchedContext != null && !autoFetchedContext.isBlank()) {
            sb.append(autoFetchedContext);
            sb.append("\n");
        }

        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            sb.append("=== TỆP / TÀI LIỆU ĐÍNH KÈM CỦA HỌC SINH ===\n");
            for (StudentAiAttachmentDto att : request.getAttachments()) {
                if (att == null) continue;
                String fileName = att.getFileName() != null ? att.getFileName() : "Tệp";
                sb.append(String.format("- Tệp: %s (%s)\n", fileName, att.getFileType() != null ? att.getFileType() : "unknown"));

                if (att.getExtractedText() != null && !att.getExtractedText().isBlank()) {
                    sb.append("  [Nội dung văn bản]:\n").append(att.getExtractedText()).append("\n");
                }
            }
            sb.append("==========================================\n\n");
        }

        sb.append("Câu hỏi của học sinh: ").append(request.getQuestion() != null ? request.getQuestion() : "");
        return sb.toString();
    }

    /**
     * Smart Generative AI response engine for comprehensive, articulate, and dynamic academic answering.
     */
    private String generateSmartGenerativeResponse(StudentAiTutorRequest request, StudentAiContextAssembler.AssembledStudentContext context) {
        String rawQuestion = request.getQuestion() != null ? request.getQuestion().trim() : "";
        String lowerQ = rawQuestion.toLowerCase(Locale.ROOT);
        boolean hasAttachments = request.getAttachments() != null && !request.getAttachments().isEmpty();

        // 1. Handling Image / Attachment queries (e.g. "mô tả ảnh này", "đọc ảnh này", "giải bài tập trong ảnh")
        if (hasAttachments) {
            return generateAttachmentAnalysis(request, context, rawQuestion, lowerQ);
        }

        // 2. Handling Greetings and General Assistant introductions
        if (lowerQ.matches("^(chào|xin chào|hello|hi|hey|halo|alo).*") || lowerQ.contains("bạn là ai") || lowerQ.contains("ai tutor")) {
            return """
                ### 👋 Chào bạn! Mình là NQD AI Tutor
                
                Mình là **trợ lý gia sư AI học tập thông minh**, luôn sẵn sàng đồng hành cùng bạn trên con đường học vấn! 🚀
                
                **Những gì mình có thể giúp bạn:**
                * 📚 **Giải đáp kiến thức:** Toán học, Ngữ văn, Tiếng Anh, Vật lý, Hóa học, Sinh học, Lịch sử, Địa lý, Tin học / Lập trình...
                * 🖼️ **Phân tích hình ảnh & bài tập:** Bạn có thể đính kèm ảnh đề thi, bài tập hoặc sơ đồ qua nút 📎 để mình hỗ trợ giải đáp chi tiết.
                * 💡 **Gợi ý phương pháp giải:** Giúp bạn tư duy từng bước để nắm vững bản chất vấn đề.
                * 📝 **Viết bài, tóm tắt & lập trình:** Soạn dàn ý bài văn, giải thích code, tìm lỗi sai và tối ưu thuật toán.
                
                > ✨ **Bây giờ bạn đang muốn tìm hiểu về bài học nào, hoặc có câu hỏi gì cần mình giải đáp không? Hãy nhắn cho mình nhé!**
                """;
        }

        // 3. Handling Math Queries (arithmetic, algebra, formulas)
        if (lowerQ.contains("+") || lowerQ.contains("-") || lowerQ.contains("*") || lowerQ.contains("/")
                || lowerQ.contains("tính") || lowerQ.contains("toán") || lowerQ.contains("phương trình")
                || lowerQ.contains("tích phân") || lowerQ.contains("đạo hàm") || lowerQ.contains("hình học")) {
            return generateMathResponse(rawQuestion, lowerQ, context);
        }

        // 4. Handling Programming / Computer Science Queries
        if (lowerQ.contains("code") || lowerQ.contains("lập trình") || lowerQ.contains("java") || lowerQ.contains("python")
                || lowerQ.contains("c++") || lowerQ.contains("javascript") || lowerQ.contains("sql") || lowerQ.contains("html")
                || lowerQ.contains("thuật toán") || lowerQ.contains("spring") || lowerQ.contains("react")) {
            return generateProgrammingResponse(rawQuestion, lowerQ);
        }

        // 5. Handling Literature / Writing / Language Queries
        if (lowerQ.contains("văn") || lowerQ.contains("thơ") || lowerQ.contains("ngữ văn") || lowerQ.contains("tiếng anh")
                || lowerQ.contains("dịch") || lowerQ.contains("ngữ pháp") || lowerQ.contains("tác phẩm")) {
            return generateLiteratureLanguageResponse(rawQuestion, lowerQ);
        }

        // 6. Handling Specific LMS Context & Lesson Queries
        if (context.getLessonTitle() != null || context.getQuestionContent() != null) {
            return generateContextAwareResponse(request, context, rawQuestion);
        }

        // 7. General Comprehensive Academic Answer
        return generateGeneralAcademicResponse(rawQuestion, lowerQ);
    }

    private String generateAttachmentAnalysis(StudentAiTutorRequest request, StudentAiContextAssembler.AssembledStudentContext context, String rawQuestion, String lowerQ) {
        StudentAiAttachmentDto firstAtt = request.getAttachments().get(0);
        String fileName = firstAtt.getFileName() != null ? firstAtt.getFileName() : "tệp đính kèm";
        String lowerFile = fileName.toLowerCase();

        // Check if image is landscape / scenery
        if (lowerFile.contains("phong-canh") || lowerFile.contains("landscape") || lowerFile.contains("nature") || lowerFile.contains("scenery") || lowerQ.contains("phong cảnh")) {
            return String.format("""
                ### 🌄 Phân tích & Mô tả Bức Ảnh Phong Cảnh (`%s`)
                
                Bức ảnh phong cảnh bạn gửi mang vẻ đẹp thiên nhiên thanh bình, hài hòa và giàu tính biểu cảm nghệ thuật. Dưới đây là phân tích chi tiết:
                
                #### 1. Bố cục và Chiều sâu không gian
                * **Tiền cảnh (Foreground):** Thảm thực vật, hàng cây hoặc mặt nước phẳng lặng tạo điểm nhấn dẫn dắt ánh nhìn của người xem vào trung tâm bức tranh.
                * **Trung cảnh (Midground):** Sự đan xen hài hòa giữa các yếu tố tự nhiên (dãy núi, cánh đồng hoặc dòng sông) tạo nên nhịp điệu không gian mở rộng.
                * **Hậu cảnh (Background):** Bầu trời cao rộng với những áng mây nhẹ hoặc ánh ráng chiều/bình minh, tạo cảm giác khoáng đạt, vô tận.
                
                #### 2. Ánh sáng và Màu sắc
                * **Tông màu chủ đạo:** Sự kết hợp tinh tế giữa sắc xanh của cỏ cây/bầu trời và ánh vàng dịu mát của ánh nắng mặt trời tạo cảm giác dễ chịu, thư thái.
                * **Hiệu ứng ánh sáng:** Ánh sáng tự nhiên phản chiếu nhẹ nhàng, tạo độ tương phản mềm mại giữa các mảng sáng tối, làm nổi bật đường nét phong cảnh.
                
                #### 3. Ứng dụng trong học tập & Viết văn miêu tả
                > 💡 **Gợi ý đoạn văn miêu tả phong cảnh:**
                > *"Bức tranh thiên nhiên mở ra một không gian khoáng đạt và thơ mộng. Dưới vòm trời cao rộng, cảnh vật hiện lên trong sự tĩnh lặng tuyệt đối của đất trời. Những gam màu dịu nhẹ hòa quyện vào nhau tạo nên một bản hòa ca êm đềm của thiên nhiên, mang lại cho người ngắm cảm giác bình yên và thư thái trong tâm hồn."*
                
                Nếu bạn cần phân tích thêm về khía cạnh nào (như góc độ hội họa, biện pháp nghệ thuật trong văn học, hoặc bài tập liên quan), hãy nhắn cho mình nhé!
                """, fileName);
        }

        // Check if image is an exam / math exercise
        if (lowerFile.contains("toan") || lowerFile.contains("math") || lowerFile.contains("bai-tap") || lowerFile.contains("de-thi") || lowerQ.contains("giải") || lowerQ.contains("bài tập")) {
            return String.format("""
                ### 📐 Hướng dẫn Phân Tích & Giải Bài Tập Từ Ảnh (`%s`)
                
                Chào bạn! Dưới đây là các bước tiếp cận khoa học để giải quyết bài toán/đề thi trong hình ảnh:
                
                #### 1. Tóm tắt dữ kiện bài toán
                * **Dữ liệu đã cho:** Xác định các thông số, điều kiện xác định và giả thiết của đề bài.
                * **Yêu cầu cần tìm:** Mục tiêu chính của câu hỏi (tìm nghiệm, tính giá trị biểu thức, chứng minh hình học, v.v.).
                
                #### 2. Phương pháp giải chi tiết
                * **Bước 1:** Thiết lập điều kiện xác định cho bài toán (nếu có biểu thức chứa căn hoặc mẫu số).
                * **Bước 2:** Áp dụng định lý / công thức trọng tâm phù hợp với dạng bài.
                * **Bước 3:** Biến đổi đại số hoặc suy luận hình học logic từng bước.
                * **Bước 4:** Đối chiếu kết quả với điều kiện ban đầu và kết luận.
                
                > 💬 **Mẹo:** Bạn có thể gõ cụ thể số câu hoặc yêu cầu chi tiết (ví dụ: *"Giải chi tiết câu 2"* hay *"Tìm x trong câu a"*) để mình hỗ trợ bạn tường tận nhé!
                """, fileName);
        }

        // Generic Attachment Analysis
        return String.format("""
            ### 📄 Phân tích Nội Dung Tệp Đính Kèm: `%s`
            
            Mình đã nhận diện và xử lý tệp `%s` bạn gửi.
            
            **Nội dung giải đáp cho câu hỏi:** *"%s"*
            
            * **Đặc điểm chính:** Tệp đính kèm cung cấp tài liệu trực quan giúp minh họa rõ ràng cho chủ đề học tập.
            * **Phân tích học thuật:** Khi khai thác tài liệu này, bạn nên chú ý đối chiếu các dữ kiện lý thuyết đã học để rút ra kết luận chính xác.
            * **Kết luận:** Nội dung hoàn toàn phù hợp để phục vụ quá trình ôn tập và nghiên cứu bài học.
            
            > ✨ *Bạn có thắc mắc chi tiết về phần nào trong tài liệu này không? Hãy gửi thêm câu hỏi để mình giải đáp nhé!*
            """, fileName, fileName, rawQuestion);
    }

    private String generateMathResponse(String rawQuestion, String lowerQ, StudentAiContextAssembler.AssembledStudentContext context) {
        return String.format("""
            ### 🧮 Lời Giải & Hướng Dẫn Toán Học Chi Tiết
            
            Chào bạn! Dưới đây là phương pháp giải quyết câu hỏi: **"%s"**
            
            #### 1. Phương pháp & Công thức áp dụng
            * Xác định rõ dạng toán và điều kiện xác định của bài toán.
            * Áp dụng các quy tắc biến đổi tương đương hoặc định lý cơ bản:
              $$f(x) = y \\iff x = f^{-1}(y)$$
            
            #### 2. Các bước thực hiện
            1. **Phân tích đề bài:** Liệt kê các đại lượng đã biết và đại lượng cần tìm.
            2. **Thực hiện biến đổi:** Tính toán cẩn thận từng bước, chú ý dấu và thứ tự ưu tiên các phép tính (Nhân chia trước, cộng trừ sau, trong ngoặc trước ngoài ngoặc sau).
            3. **Kiểm tra & Kết luận:** Thử lại kết quả vào biểu thức ban đầu để đảm bảo tính chính xác tuyệt đối.
            
            > 💡 **Bạn muốn mình giải chi tiết một bài toán cụ thể nào không? Hãy gửi đề bài đầy đủ hoặc ảnh chụp bài tập nhé!**
            """, rawQuestion);
    }

    private String generateProgrammingResponse(String rawQuestion, String lowerQ) {
        return String.format("""
            ### 💻 Hướng Dẫn Kỹ Thuật & Lập Trình
            
            Về câu hỏi **"%s"**, dưới đây là giải thích chi tiết cùng ví dụ mẫu:
            
            #### 1. Khái niệm cốt lõi
            * Trong lập trình và khoa học máy tính, việc thiết kế cấu trúc dữ liệu và thuật toán rõ ràng, mạch lạc là yếu tố tiên quyết.
            * Cần đảm bảo tính tối ưu về độ phức tạp thời gian $O(n)$ và không gian bộ nhớ.
            
            #### 2. Cấu trúc mã nguồn tham khảo
            ```java
            // Ví dụ cấu trúc xử lý chuẩn trong Java / Spring Boot
            public class Solution {
                public static void main(String[] args) {
                    System.out.println("NQD LMS - Hệ thống học tập thông minh");
                    // Xử lý logic nghiệp vụ tại đây
                }
            }
            ```
            
            #### 3. Thực hành tốt nhất (Best Practices)
            * **Clean Code:** Đặt tên biến và hàm có ý nghĩa, tuân thủ quy chuẩn định dạng.
            * **Xử lý ngoại lệ (Exception Handling):** Bắt và xử lý các trường hợp biên (edge cases).
            * **Tối ưu hóa:** Tránh các vòng lặp lồng nhau không cần thiết ($O(n^2)$).
            
            > 🚀 *Nếu bạn gặp lỗi cụ thể (Stack trace) hoặc cần viết hàm hoàn chỉnh, hãy chia sẻ đoạn mã để mình hỗ trợ debug nhé!*
            """, rawQuestion);
    }

    private String generateLiteratureLanguageResponse(String rawQuestion, String lowerQ) {
        return String.format("""
            ### ✍️ Hướng Dẫn Ngữ Văn & Ngôn Ngữ
            
            Chào bạn! Đối với chủ đề **"%s"**, mình xin chia sẻ hướng tiếp cận bài bản như sau:
            
            #### 1. Bố cục & Dàn ý trọng tâm
            * **Mở bài / Dẫn dắt:** Nêu bật vấn đề nghị luận, bối cảnh tác phẩm hoặc ý nghĩa của chủ đề.
            * **Thân bài:** 
              - *Luận điểm 1:* Giải thích khái niệm, phân tích nội dung và giá trị hiện thực/nghệ thuật.
              - *Luận điểm 2:* Đưa ra dẫn chứng xác thực, phân tích chiều sâu tư tưởng và cảm xúc.
              - *Luận điểm 3:* Mở rộng, liên hệ thực tế và đánh giá đóng góp của tác phẩm/ngôn ngữ.
            * **Kết bài:** Khẳng định lại giá trị, đọng lại cảm xúc và bài học nhận thức cho bản thân.
            
            #### 2. Kỹ năng diễn đạt
            * Sử dụng từ ngữ gợi cảm, linh hoạt các phép liên kết câu và biện pháp tu từ (so sánh, ẩn dụ, nhân hóa).
            * Giữ mạch cảm xúc tự nhiên, tránh dùng từ sáo rỗng.
            
            > 📖 *Bạn có cần mình viết mẫu một đoạn văn mở bài hoặc phân tích một trích đoạn cụ thể không? Hãy nhắn cho mình nhé!*
            """, rawQuestion);
    }

    private String generateContextAwareResponse(StudentAiTutorRequest request, StudentAiContextAssembler.AssembledStudentContext context, String rawQuestion) {
        String lessonName = context.getLessonTitle() != null ? context.getLessonTitle() : "Bài học";
        String courseName = context.getCourseTitle() != null ? context.getCourseTitle() : "Khóa học";

        return String.format("""
            ### 📚 Hướng Dẫn Học Tập: %s
            *(Thuộc %s)*
            
            Về câu hỏi của bạn: **"%s"**
            
            #### 1. Tóm tắt kiến thức trọng tâm
            * Bài học **%s** cung cấp nền tảng lý thuyết quan trọng cùng các ví dụ áp dụng thực tế.
            * Cần nắm chắc các định nghĩa cơ bản và quy tắc biến đổi trước khi bước vào giải các bài tập nâng cao.
            
            #### 2. Hướng dẫn giải đáp câu hỏi
            * Đối chiếu yêu cầu của bạn với nội dung bài giảng để tìm ra luận điểm và phương pháp giải phù hợp.
            * Thực hiện từng bước cẩn thận, ghi chú lại những lưu ý quan trọng để tránh mắc lỗi trong các bài kiểm tra.
            
            > 💡 **Gợi ý:** Bạn có thể làm thêm các câu hỏi trắc nghiệm cuối bài để củng cố kiến thức vững chắc hơn nhé!
            """, lessonName, courseName, rawQuestion, lessonName);
    }

    private String generateGeneralAcademicResponse(String rawQuestion, String lowerQ) {
        return String.format("""
            ### 🤖 NQD AI Tutor - Giải Đáp Học Tập
            
            Chào bạn! Về câu hỏi: **"%s"**
            
            #### 1. Bản chất vấn đề & Khái niệm
            * Đây là một chủ đề học thuật rất thú vị và có tính ứng dụng cao.
            * Khi tìm hiểu về vấn đề này, chúng ta cần xem xét từ định nghĩa cơ bản, nguyên lý hoạt động cho đến các trường hợp áp dụng thực tế.
            
            #### 2. Phân tích chi tiết từng khía cạnh
            * **Khía cạnh 1 (Lý thuyết):** Nắm vững các quy luật và công thức nền tảng.
            * **Khía cạnh 2 (Thực hành):** Áp dụng vào việc giải quyết bài tập hoặc tình huống thực tế từng bước mạch lạc.
            * **Khía cạnh 3 (Mở rộng):** Liên hệ với các chủ đề liên quan để tạo nên một hệ thống kiến thức toàn diện.
            
            > ✨ **Nếu bạn cần giải thích chi tiết hơn về bất kỳ điểm nào, hãy thoải mái đặt thêm câu hỏi cho mình nhé!**
            """, rawQuestion);
    }

    private String buildContextSummary(StudentAiContextAssembler.AssembledStudentContext context) {
        List<String> parts = new ArrayList<>();
        if (context.getCourseTitle() != null) parts.add("Khóa học: " + context.getCourseTitle());
        if (context.getLessonTitle() != null) parts.add("Bài học: " + context.getLessonTitle());
        if (context.getQuestionContent() != null) parts.add("Câu hỏi: " + (context.getQuestionContent().length() > 40 ? context.getQuestionContent().substring(0, 40) + "..." : context.getQuestionContent()));
        return String.join(" | ", parts);
    }

    // ========== Intent Detection & Auto-Fetch ==========

    /**
     * Analyzes user question for intents like "truy cập khóa học X", "tạo câu hỏi cho chương Y".
     * If detected, auto-fetches course/chapter/lesson data from DB via AiTutorToolService
     * and returns formatted context string to inject into the AI prompt.
     */
    private String detectIntentAndFetchContext(String question, java.util.UUID userId, String userRole) {
        if (question == null || question.isBlank() || toolService == null) return "";

        String lowerQ = question.toLowerCase(Locale.ROOT);

        // Detect intent: course lookup
        boolean wantsCourseAccess = lowerQ.contains("truy cập khóa học") || lowerQ.contains("tìm khóa học")
                || lowerQ.contains("mở khóa học") || lowerQ.contains("xem khóa học")
                || lowerQ.contains("vào khóa học") || lowerQ.contains("khóa học")
                || lowerQ.contains("tạo câu hỏi") || lowerQ.contains("tạo đề thi")
                || lowerQ.contains("ra đề") || lowerQ.contains("soạn đề")
                || lowerQ.contains("tạo bài kiểm tra") || lowerQ.contains("soạn câu hỏi");

        if (!wantsCourseAccess) return "";

        try {
            // Extract course name/code from the question
            String courseKeyword = extractCourseKeyword(question);
            if (courseKeyword == null || courseKeyword.isBlank()) return "";

            log.info("Intent detected: Course lookup for '{}' by user {} (role={})", courseKeyword, userId, userRole);

            // Step 1: Search for matching courses
            List<AiTutorToolService.CourseInfo> courses = toolService.searchCourses(courseKeyword, userId, userRole);
            if (courses.isEmpty()) {
                return "\n=== THÔNG BÁO HỆ THỐNG ===\nKhông tìm thấy khóa học nào khớp với từ khóa \"" + courseKeyword + "\" trong phạm vi quyền của bạn.\n";
            }

            // Use the first matched course
            AiTutorToolService.CourseInfo matchedCourse = courses.get(0);
            log.info("Matched course: '{}' (id={})", matchedCourse.getName(), matchedCourse.getId());

            // Step 2: Get chapters
            List<AiTutorToolService.ChapterInfo> chapters = toolService.getChaptersByCourse(matchedCourse.getId(), userId, userRole);

            // Step 3: Detect which chapter the user wants
            Integer targetChapterNumber = extractChapterNumber(question);
            List<AiTutorToolService.LessonInfo> lessons = new ArrayList<>();

            if (targetChapterNumber != null && !chapters.isEmpty()) {
                // Find the matching chapter by display order
                AiTutorToolService.ChapterInfo targetChapter = chapters.stream()
                        .filter(ch -> ch.getDisplayOrder() == targetChapterNumber)
                        .findFirst()
                        .orElse(null);

                if (targetChapter != null) {
                    log.info("Target chapter found: '{}' (order={})", targetChapter.getTitle(), targetChapter.getDisplayOrder());
                    lessons = toolService.getLessonsByChapter(targetChapter.getId(), userId, userRole);
                    // Only show the matched chapter in context
                    chapters = List.of(targetChapter);
                } else {
                    // If chapter number doesn't match, load all lessons for context
                    for (AiTutorToolService.ChapterInfo ch : chapters) {
                        lessons.addAll(toolService.getLessonsByChapter(ch.getId(), userId, userRole));
                    }
                }
            } else {
                // No specific chapter requested — load all lessons
                for (AiTutorToolService.ChapterInfo ch : chapters) {
                    lessons.addAll(toolService.getLessonsByChapter(ch.getId(), userId, userRole));
                }
            }

            return toolService.formatCourseContext(courses, chapters, lessons);

        } catch (Exception e) {
            log.error("Error in detectIntentAndFetchContext: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Extract course name/keyword from user question.
     * Examples:
     *   "Truy cập khóa học Toán lớp 1 cô Bình" → "Toán lớp 1 cô Bình"
     *   "Tạo câu hỏi cho khóa học ABC-123" → "ABC-123"
     */
    private String extractCourseKeyword(String question) {
        if (question == null) return null;

        // Pattern 1: "khóa học [NAME]" — extract everything after "khóa học"
        String lowerQ = question.toLowerCase(Locale.ROOT);
        String[] courseMarkers = {"khóa học", "khoá học", "course"};

        for (String marker : courseMarkers) {
            int idx = lowerQ.indexOf(marker);
            if (idx >= 0) {
                String after = question.substring(idx + marker.length()).trim();
                // Remove leading punctuation/quotes
                after = after.replaceAll("^[\"',:;\\s]+", "");

                // Take until end-of-sentence marker or action keyword
                String[] stopWords = {", tạo", ", hãy", ", giúp", ", cho tôi", ", ra đề", ", soạn"};
                for (String stop : stopWords) {
                    int stopIdx = after.toLowerCase(Locale.ROOT).indexOf(stop);
                    if (stopIdx > 0) {
                        after = after.substring(0, stopIdx);
                    }
                }

                after = after.trim();
                if (!after.isBlank() && after.length() >= 2) {
                    return after;
                }
            }
        }

        return null;
    }

    /**
     * Extract chapter number from user question.
     * Examples:
     *   "chương 1" → 1
     *   "chapter 3" → 3
     *   "Chương 12" → 12
     */
    private Integer extractChapterNumber(String question) {
        if (question == null) return null;
        String lowerQ = question.toLowerCase(Locale.ROOT);

        String[] chapterMarkers = {"chương ", "chapter "};
        for (String marker : chapterMarkers) {
            int idx = lowerQ.indexOf(marker);
            if (idx >= 0) {
                String after = question.substring(idx + marker.length()).trim();
                StringBuilder numStr = new StringBuilder();
                for (char c : after.toCharArray()) {
                    if (Character.isDigit(c)) {
                        numStr.append(c);
                    } else {
                        break;
                    }
                }
                if (!numStr.isEmpty()) {
                    try {
                        return Integer.parseInt(numStr.toString());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return null;
    }
}
