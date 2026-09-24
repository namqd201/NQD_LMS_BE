package com.nqd.nqd_lms_be.ai;

import com.nqd.nqd_lms_be.dto.student.StudentAiTutorMode;
import com.nqd.nqd_lms_be.dto.student.StudentAiTutorRequest;
import com.nqd.nqd_lms_be.dto.student.StudentAiTutorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StudentAiTutorPedagogicalPolicyTest {

    private AiTutorPolicyConfig policyConfig;
    private StudentAiTutorEngine engine;

    @BeforeEach
    void setUp() {
        policyConfig = new AiTutorPolicyConfig();
        policyConfig.loadPolicy();

        // Instantiate engine without live API keys to test fallback smart generative engine
        engine = new StudentAiTutorEngine(
                "", "gemini-3.6-flash",
                "", "gpt-4o-mini",
                "https://api.openai.com/v1",
                null,
                policyConfig,
                null
        );
    }

    @Test
    @DisplayName("AiTutorPolicyConfig correctly loads pedagogical rules and includes them in prompt")
    void testPolicyConfigPedagogicalRules() {
        assertFalse(policyConfig.getPedagogicalRules().isEmpty(), "Pedagogical rules should not be empty");
        String promptSection = policyConfig.buildPolicyPromptSection("STUDENT");
        assertTrue(promptSection.contains("NGUYÊN TẮC SƯ PHẠM ĐỐI VỚI CÁC MÔN TỰ NHIÊN"));
        assertTrue(promptSection.contains("TUYỆT ĐỐI KHÔNG giải trọn vẹn"));
    }

    @Test
    @DisplayName("Math question response guides step-by-step and does NOT give final answer directly")
    void testMathGuidanceNotFullySolved() {
        StudentAiTutorRequest req = StudentAiTutorRequest.builder()
                .mode(StudentAiTutorMode.GENERAL_QA)
                .question("Giải phương trình 2x + 4 = 10")
                .build();

        StudentAiContextAssembler.AssembledStudentContext context = StudentAiContextAssembler.AssembledStudentContext.builder()
                .subjectName("Toán học")
                .build();

        StudentAiTutorResponse response = engine.generateTutorResponse(req, context, 20);

        assertNotNull(response);
        assertNotNull(response.getAnswer());

        String answer = response.getAnswer();
        // Verifies step-by-step guidance is present
        assertTrue(answer.contains("Hướng Dẫn Từng Bước Giải Bài Toán") || answer.contains("Phân tích"));
        assertTrue(answer.contains("Bước tính toán cuối cùng dành cho bạn") || answer.contains("tự mình thực hiện"));
        assertTrue(answer.contains("nhắn lại đáp số"));

        // Verifies it does NOT prematurely provide direct finished answer
        assertFalse(answer.contains("x = 3 là nghiệm duy nhất"));
    }

    @Test
    @DisplayName("Physics question response guides step-by-step and leaves final computation for student")
    void testPhysicsGuidanceNotFullySolved() {
        StudentAiTutorRequest req = StudentAiTutorRequest.builder()
                .mode(StudentAiTutorMode.GENERAL_QA)
                .question("Một vật khối lượng 2kg chuyển động với gia tốc a = 3m/s2. Tính lực tác dụng F?")
                .build();

        StudentAiContextAssembler.AssembledStudentContext context = StudentAiContextAssembler.AssembledStudentContext.builder()
                .subjectName("Vật lý")
                .build();

        StudentAiTutorResponse response = engine.generateTutorResponse(req, context, 20);

        assertNotNull(response);
        assertNotNull(response.getAnswer());

        String answer = response.getAnswer();
        // Verifies Physics guidance
        assertTrue(answer.contains("Bài Tập Vật Lý"));
        assertTrue(answer.contains("Tóm tắt đề bài") || answer.contains("Định luật & Công thức"));
        assertTrue(answer.contains("Bước tính toán cuối cùng dành cho bạn"));
        assertTrue(answer.contains("Gửi đáp số cho mình") || answer.contains("nhắn lại đáp án"));

        // Verifies it does NOT give direct final answer "F = 6N"
        assertFalse(answer.contains("F = 6N") || answer.contains("F = 6 N"));
    }

    @Test
    @DisplayName("When student explicitly demands direct answer, AI politely refuses and encourages thinking")
    void testRefusalWhenDemandingDirectAnswer() {
        StudentAiTutorRequest req = StudentAiTutorRequest.builder()
                .mode(StudentAiTutorMode.GENERAL_QA)
                .question("Cho em đáp án luôn đi, không cần giải thích!")
                .build();

        StudentAiContextAssembler.AssembledStudentContext context = StudentAiContextAssembler.AssembledStudentContext.builder()
                .build();

        StudentAiTutorResponse response = engine.generateTutorResponse(req, context, 20);

        assertNotNull(response);
        assertNotNull(response.getAnswer());

        String answer = response.getAnswer();
        assertTrue(answer.contains("Lời Khuyên Sư Phạm Dành Cho Bạn"));
        assertTrue(answer.contains("nguyên tắc sư phạm"));
        assertTrue(answer.contains("tự mình thực hiện"));
    }
}
