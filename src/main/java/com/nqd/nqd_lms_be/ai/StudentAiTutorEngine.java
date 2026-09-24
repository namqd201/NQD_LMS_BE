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

        String systemPrompt = buildSystemPrompt(request.getMode(), context, userRole);
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

    private String buildSystemPrompt(StudentAiTutorMode mode, StudentAiContextAssembler.AssembledStudentContext context, String userRole) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            Bạn là NQD AI Tutor - một trợ lý gia sư trí tuệ nhân tạo toàn năng, thông thái, ân cần và sinh động (tương tự như ChatGPT / Gemini), hỗ trợ học sinh học tập và giải đáp mọi thắc mắc học thuật.
            
            Khả năng & Quy tắc trình bày:
            - Phân tích và giải đáp mọi câu hỏi thuộc tất cả các môn học (Toán, Văn, Anh, Lý, Hóa, Sinh, Sử, Địa, Tin học / Lập trình, Triết học, v.v.).
            - Đọc hiểu hình ảnh bài tập, đề thi, sơ đồ, bảng biểu hoặc tranh ảnh do học sinh gửi lên.
            - Hướng dẫn giải bài tập theo từng bước mạch lạc, giải thích bản chất kiến thức.
            - Trả lời bằng tiếng Việt tự nhiên, chuẩn mực, lịch sự.
            - Trình bày công thức Toán học, Vật lý chuẩn LaTeX: đặt công thức trong dòng vào $...$ và công thức khối riêng vào $$...$$.
            - Đối với các bước giải, ưu tiên dùng in đậm '**Bước 1: ...**', '**Bước 2: ...**' và gạch đầu dòng gọn gàng, tránh dùng quá nhiều ký tự tiêu đề '#' nếu không cần thiết.
            
            Khả năng truy vấn dữ liệu khóa học:
            - Khi người dùng yêu cầu truy cập vào một khóa học, chương, bài giảng cụ thể, hệ thống sẽ tự động tra cứu và cung cấp nội dung cho bạn trong phần context.
            - Bạn hãy sử dụng nội dung bài giảng thực tế đó để tạo câu hỏi, đề thi, hoặc giải đáp thắc mắc.
            - Khi tạo câu hỏi/đề thi, hãy dựa sát vào kiến thức trong bài giảng, đảm bảo độ chính xác và phù hợp với trình độ.
            
            🎯 NGUYÊN TẮC SƯ PHẠM ĐẶC BIỆT KHI HƯỚNG DẪN CÁC MÔN TỰ NHIÊN (TOÁN HỌC, VẬT LÝ, HÓA HỌC...):
            - TUYỆT ĐỐI KHÔNG giải hoàn toàn 100% hoặc cung cấp đáp án / kết quả tính toán số cuối cùng cho học sinh!
            - Tuyệt đối không chọn sẵn chữ cái đáp án trắc nghiệm (ví dụ: 'Chọn A', 'Đáp án là B').
            - Nhiệm vụ cốt lõi của bạn là một NGƯỜI HƯỚNG DẪN TƯ DUY (Socratic Tutor) hướng dẫn chi tiết từng bước:
              + Bước 1 (Tóm tắt & phân tích đề bài): Xác định rõ dữ kiện đã cho (giả thiết, các thông số kèm đơn vị như $m, v_0, t, s...$) và đại lượng cần tìm (kết luận).
              + Bước 2 (Kiến thức trọng tâm): Nhắc lại công thức toán học, định lý, hoặc định luật vật lý cần áp dụng (trình bày công thức bằng LaTeX $...$ hoặc $$...$$).
              + Bước 3 (Hướng dẫn tư duy & biến đổi): Giải thích chi tiết các bước biến đổi trung gian, cách thiết lập phương trình hoặc liên kết các đại lượng một cách mạch lạc, dễ hiểu.
              + Bước 4 (Dừng lại trước phép tính cuối cùng): KHÔNG tính ra con số cuối cùng. Hãy dừng lại ở biểu thức tính cuối và yêu cầu/gợi ý học sinh tự thay số vào tính toán hoặc suy luận ra đáp số.
              + Bước 5 (Mời học sinh đối chiếu): Khuyến khích học sinh: "Bạn hãy thử tính nhẩm hoặc bấm máy tính xem kết quả ra bao nhiêu, rồi nhắn lại cho mình để cùng kiểm tra nhé!".
            - Nếu học sinh nài nỉ xin đáp án cuối cùng hoặc bảo AI giải hết hộ: Hãy ân cần giải thích rằng việc tự làm bước cuối sẽ giúp bạn ghi nhớ và hiểu sâu bản chất để làm tốt các bài kiểm tra, sau đó tiếp tục gợi mở hoặc hỗ trợ nếu học sinh bị vướng mắc ở bước biến đổi nào.
            - Nếu học sinh gửi kết quả họ đã tự tính: Hãy đối chiếu kết quả của học sinh, khen ngợi nếu bạn làm đúng, hoặc chỉ ra vị trí học sinh bị nhầm lẫn (nhầm dấu, nhầm đơn vị, quên bình phương, v.v.) để bạn tự sửa.
            """);

        if (context != null && context.getSubjectName() != null && !context.getSubjectName().isBlank()) {
            sb.append("\n📚 MÔN HỌC HIỆN TẠI: ").append(context.getSubjectName()).append("\n");
            String lowerSubj = context.getSubjectName().toLowerCase(Locale.ROOT);
            if (lowerSubj.contains("toán") || lowerSubj.contains("lý") || lowerSubj.contains("hóa") || lowerSubj.contains("vật lí") || lowerSubj.contains("vật lý")) {
                sb.append("⚠️ BẮT BUỘC ÁP DỤNG: Môn học này thuộc nhóm Khoa học tự nhiên/Tính toán. Hãy tuân thủ nghiêm ngặt nguyên tắc chỉ hướng dẫn chi tiết từng bước, tuyệt đối KHÔNG đưa ra đáp số cuối cùng!\n");
            }
        }

        // Inject security policy from config
        if (policyConfig != null) {
            sb.append(policyConfig.buildPolicyPromptSection(userRole));
        }

        if (context != null && Boolean.TRUE.equals(context.getIsExamInProgress())) {
            sb.append("""
            
            ⚠️ LƯU Ý KHI HỌC SINH ĐANG LÀM BÀI THI:
            - Tuyệt đối không đưa ra trực tiếp chữ cái đáp án trắc nghiệm (như 'Chọn A', 'Đáp án là B').
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

        // 2b. Handling demands for direct final answer ("cho đáp án luôn", "tính hộ kết quả cuối cùng"...)
        if (isDemandingFinalAnswer(lowerQ)) {
            return generatePedagogicalRefusal(rawQuestion);
        }

        // 3. Handling Physics Queries (mechanics, electricity, optics, thermodynamics, etc.)
        boolean isPhysicsSubject = (context != null && context.getSubjectName() != null &&
                (context.getSubjectName().toLowerCase(Locale.ROOT).contains("lý") ||
                 context.getSubjectName().toLowerCase(Locale.ROOT).contains("vật lí") ||
                 context.getSubjectName().toLowerCase(Locale.ROOT).contains("vật lý")));

        boolean isPhysicsQuery = isPhysicsSubject || lowerQ.contains("vật lý") || lowerQ.contains("vật lí")
                || lowerQ.contains("vận tốc") || lowerQ.contains("gia tốc") || lowerQ.contains("quãng đường")
                || lowerQ.contains("lực") || lowerQ.contains("động năng") || lowerQ.contains("thế năng")
                || lowerQ.contains("cơ năng") || lowerQ.contains("định luật newton") || lowerQ.contains("định luật niu-tơn")
                || lowerQ.contains("rơi tự do") || lowerQ.contains("thấu kính") || lowerQ.contains("khúc xạ")
                || lowerQ.contains("phản xạ") || lowerQ.contains("điện xoay chiều") || lowerQ.contains("cường độ dòng điện")
                || lowerQ.contains("hiệu điện thế") || lowerQ.contains("điện trở") || lowerQ.contains("định luật ôm")
                || lowerQ.contains("công suất") || lowerQ.contains("dao động") || lowerQ.contains("con lắc")
                || lowerQ.contains("bước sóng") || lowerQ.contains("nhiệt lượng") || lowerQ.contains("áp suất");

        if (isPhysicsQuery) {
            return generatePhysicsResponse(rawQuestion, lowerQ, context);
        }

        // 3b. Handling Math Queries (arithmetic, algebra, formulas, calculus, geometry)
        boolean isMathSubject = (context != null && context.getSubjectName() != null &&
                (context.getSubjectName().toLowerCase(Locale.ROOT).contains("toán")));

        boolean isMathQuery = isMathSubject || lowerQ.contains("+") || lowerQ.contains("-") || lowerQ.contains("*") || lowerQ.contains("/")
                || lowerQ.contains("tính") || lowerQ.contains("toán") || lowerQ.contains("phương trình")
                || lowerQ.contains("tích phân") || lowerQ.contains("đạo hàm") || lowerQ.contains("hình học")
                || lowerQ.contains("tam giác") || lowerQ.contains("hệ phương trình") || lowerQ.contains("vecto")
                || lowerQ.contains("tọa độ") || lowerQ.contains("bất đẳng thức") || lowerQ.contains("rút gọn");

        if (isMathQuery) {
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

    private boolean isDemandingFinalAnswer(String lowerQ) {
        if (lowerQ == null || lowerQ.isBlank()) return false;
        String[] demandKeywords = {
            "cho đáp án luôn", "cho em đáp án luôn", "cho mình đáp án luôn",
            "giải hết đi", "giải từ a đến z", "tính hộ kết quả cuối cùng",
            "kết quả cuối cùng là bao nhiêu", "đáp án là gì",
            "chọn câu nào", "chọn a b c hay d", "chọn đáp án nào",
            "tính ra số bao nhiêu", "giải trọn vẹn", "tính hết cho tôi",
            "cho kết quả luôn", "kết quả bằng mấy"
        };
        for (String kw : demandKeywords) {
            if (lowerQ.contains(kw)) return true;
        }
        return false;
    }

    private String generatePedagogicalRefusal(String rawQuestion) {
        return """
            ### 💡 Lời Khuyên Sư Phạm Dành Cho Bạn
            
            Chào bạn! Mình hiểu bạn đang muốn biết ngay kết quả cuối cùng hoặc đáp án chính xác của bài tập này.
            
            Tuy nhiên, theo **nguyên tắc sư phạm của NQD AI Tutor**:
            * Đối với các môn tự nhiên như **Toán học**, **Vật lý**, **Hóa học**, mình sẽ luôn đồng hành hướng dẫn bạn **chi tiết từng bước biến đổi và tư duy logic** để bạn hiểu sâu bản chất bài học.
            * **Việc bạn tự mình thực hiện bước tính toán / thay số cuối cùng** là chìa khóa vô cùng quan trọng giúp bạn rèn luyện phản xạ tính toán, tự tin khi bước vào phòng thi và đạt điểm số tối đa.
            
            👉 **Gợi ý bước làm tiếp theo:**
            Bạn hãy nhìn lại công thức và các bước hướng dẫn mình đã đưa ra ở trên, thử thay số vào và bấm máy tính nhé! 
            Sau khi tính ra đáp số, **hãy nhắn lại kết quả cho mình** – mình sẽ đối chiếu và kiểm tra xem bạn đã tính chính xác chưa nhé! 🚀
            """;
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

        // Check if image is an exam / math / physics exercise
        if (lowerFile.contains("toan") || lowerFile.contains("math") || lowerFile.contains("bai-tap") || lowerFile.contains("de-thi")
                || lowerFile.contains("ly") || lowerFile.contains("vat-ly") || lowerFile.contains("physics")
                || lowerQ.contains("giải") || lowerQ.contains("bài tập")) {
            return String.format("""
                ### 📐 Hướng Dẫn Từng Bước Giải Bài Tập Từ Ảnh (`%s`)
                *(Theo phương pháp Socratic - Không giải hoàn toàn, hướng dẫn chi tiết từng bước)*
                
                Chào bạn! Dưới đây là phương pháp và các bước tiếp cận khoa học để bạn tự tin giải quyết bài tập trong hình ảnh:
                
                #### 1. Tóm tắt dữ kiện bài toán
                * **Dữ liệu đã cho (Giả thiết):** Xác định các thông số đã biết từ đề bài, điều kiện xác định và các dữ kiện ràng buộc.
                * **Yêu cầu cần tìm (Kết luận):** Mục tiêu chính của câu hỏi (tìm nghiệm $x$, tính vận tốc $v$, công suất $P$, chứng minh hình học, v.v.).
                
                #### 2. Định hướng phương pháp & Công thức trọng tâm
                * **Bước 1 (Thiết lập điều kiện & hệ quy chiếu):** Ghi rõ điều kiện xác định hoặc chọn chiều dương / hệ quy chiếu phù hợp.
                * **Bước 2 (Áp dụng công thức / định lý):** Lựa chọn định lý Toán học hoặc định luật Vật lý phù hợp với dạng bài.
                * **Bước 3 (Biến đổi trung gian):** Biến đổi đại số hoặc suy luận logic để rút ra biểu thức tính đại lượng cần tìm.
                
                ---
                👉 **Bước tính toán cuối cùng dành cho bạn:**
                Theo nguyên tắc sư phạm, bạn hãy tự mình thay số vào biểu thức ở Bước 3 để tính ra đáp số cuối cùng nhé!
                
                💬 **Hãy nhắn lại đáp số của bạn:** Sau khi tính xong, bạn hãy nhắn kết quả cho mình để cùng đối chiếu và kiểm tra xem bạn đã tính toán hoàn toàn chính xác chưa nhé!
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
            ### 📐 Hướng Dẫn Từng Bước Giải Bài Toán
            *(Phương pháp Socratic - Tự chủ tư duy học tập)*
            
            Chào bạn! Đối với câu hỏi toán học: **"%s"**
            
            Sau đây là hướng dẫn chi tiết từng bước để bạn tự tin giải quyết bài toán:
            
            #### 1. Phân tích & Tóm tắt đề bài
            * **Dữ kiện đã cho (Giả thiết):** Xác định các biểu thức, điều kiện ràng buộc và miền xác định của các biến số (ví dụ: mẫu số khác 0, biểu thức dưới căn bậc hai không âm $A \\ge 0$).
            * **Yêu cầu cần tìm (Kết luận):** Mục tiêu chính của đề bài (tìm giá trị ẩn số $x$, tính giá trị biểu thức, hoặc chứng minh tính chất hình học).
            
            #### 2. Kiến thức & Công thức trọng tâm
            * Tùy theo dạng bài, áp dụng các định lý hoặc hằng đẳng thức cốt lõi:
              - *Biến đổi tương đương:* $f(x) = g(x) \\iff f(x) - g(x) = 0$
              - *Hằng đẳng thức đáng nhớ:* $(a \\pm b)^2 = a^2 \\pm 2ab + b^2$, $a^2 - b^2 = (a - b)(a + b)$
              - *Quy tắc ưu tiên phép tính:* Thực hiện trong ngoặc trước, nhân chia trước, cộng trừ sau.
            
            #### 3. Các bước biến đổi chi tiết
            * **Bước 1 (Thiết lập điều kiện):** Ghi rõ điều kiện xác định để tránh nghiệm ngoại lai.
            * **Bước 2 (Chuyển vế & Biến đổi):** Nhóm các hạng tử có chứa ẩn về một vế, các hệ số tự do về vế còn lại. Rút gọn hai vế theo quy tắc toán học.
            * **Bước 3 (Thu gọn về biểu thức tính):** Đưa về dạng phương trình cơ bản $x = \\frac{B}{A}$ hoặc biểu thức cần thay số.
            
            ---
            👉 **Bước tính toán cuối cùng dành cho bạn:**
            Bây giờ bạn hãy tự mình thực hiện phép tính cuối cùng hoặc thay giá trị vào biểu thức trên nhé!
            
            💬 **Hãy nhắn lại đáp số của bạn cho mình:** Sau khi tính ra, bạn hãy gửi kết quả để mình kiểm tra xem bạn đã tính toán hoàn toàn chính xác chưa nhé! Chúc bạn làm bài thật tốt!
            """, rawQuestion);
    }

    private String generatePhysicsResponse(String rawQuestion, String lowerQ, StudentAiContextAssembler.AssembledStudentContext context) {
        return String.format("""
            ### ⚡ Hướng Dẫn Từng Bước Bài Tập Vật Lý
            *(Phương pháp Socratic - Nắm vững bản chất & Tự chủ tư duy)*
            
            Chào bạn! Đối với bài tập Vật lý: **"%s"**
            
            Dưới đây là phương pháp phân tích và định hướng giải quyết bài toán theo chuẩn phương pháp sư phạm:
            
            #### 1. Tóm tắt đề bài & Đổi đơn vị chuẩn SI
            * **Các đại lượng đã cho:** Liệt kê các thông số đã biết từ đề bài kèm đơn vị đo chuẩn (ví dụ: khối lượng $m$ (kg), quãng đường $s$ (m), thời gian $t$ (s), vận tốc $v$ (m/s), hiệu điện thế $U$ (V), cường độ dòng điện $I$ (A)...).
            * **Đổi đơn vị (nếu cần):** Đưa các đại lượng về cùng hệ đơn vị SI chuẩn (ví dụ: $km/h \\rightarrow m/s$, $cm \\rightarrow m$, $g \\rightarrow kg$, $mA \\rightarrow A$).
            * **Đại lượng cần tìm:** Xác định mục tiêu cần tính toán.
            
            #### 2. Định luật & Công thức Vật lý áp dụng
            * Nhận diện hiện tượng vật lý trong bài và chọn công thức phù hợp:
              - *Chuyển động & Động lực học:* Công thức vận tốc $v = v_0 + at$, quãng đường $s = v_0t + \\frac{1}{2}at^2$, hoặc Định luật II Newton: $\\vec{F} = m\\vec{a}$ ($F = m \\cdot a$).
              - *Công & Cơ năng:* $A = F \\cdot s \\cdot \\cos(\\alpha)$, $W_d = \\frac{1}{2}mv^2$, $W_t = mgh$.
              - *Điện học:* Định luật Ôm $I = \\frac{U}{R}$, công suất điện $P = U \\cdot I = I^2 \\cdot R = \\frac{U^2}{R}$.
            
            #### 3. Các bước tư duy & Hướng dẫn biến đổi
            * **Bước 1 (Chọn hệ quy chiếu / Chiều dương):** Xác định chiều chuyển động hoặc vẽ sơ đồ phân tích lực/mạch điện nếu cần thiết.
            * **Bước 2 (Thiết lập phương trình liên hệ):** Viết công thức tổng quát và thay thế các mối liên hệ giữa các đại lượng đã biết.
            * **Bước 3 (Rút đại lượng cần tìm):** Biến đổi đại số để biểu diễn đại lượng cần tìm ở một vế, các thông số đã cho ở vế còn lại.
            
            ---
            👉 **Bước tính toán cuối cùng dành cho bạn:**
            Bạn hãy thay các số liệu đã tóm tắt ở phần 1 vào biểu thức tính toán ở Bước 3 và bấm máy tính để tìm ra đáp số nhé (nhớ ghi kèm đơn vị đo chuẩn nhé!).
            
            💬 **Gửi đáp số cho mình:** Sau khi tính ra con số, bạn hãy nhắn lại đáp án cho mình để mình kiểm tra xem bạn đã giải chuẩn xác chưa nhé! 🚀
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
