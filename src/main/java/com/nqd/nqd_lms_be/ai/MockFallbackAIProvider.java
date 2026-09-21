package com.nqd.nqd_lms_be.ai;

import com.nqd.nqd_lms_be.ai.dto.AiQuestionGenerationPrompt;
import com.nqd.nqd_lms_be.ai.dto.ExamBlueprintItem;
import com.nqd.nqd_lms_be.ai.dto.GeneratedOptionDraft;
import com.nqd.nqd_lms_be.ai.dto.GeneratedQuestionDraft;
import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
public class MockFallbackAIProvider implements AIProvider {

    private final Random random = new Random();

    @Override
    public String getProviderName() {
        return "MOCK_FALLBACK";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<GeneratedQuestionDraft> generateQuestions(AiQuestionGenerationPrompt prompt) {
        log.info("MockFallbackAIProvider generating intelligent questions for topic: {}, subject: {}, count: {}",
                prompt.getTopic(), prompt.getSubjectName(), prompt.getCount());

        List<GeneratedQuestionDraft> result = new ArrayList<>();

        if (Boolean.TRUE.equals(prompt.getIsExamBlueprint()) && prompt.getBlueprintItems() != null && !prompt.getBlueprintItems().isEmpty()) {
            int qIndex = 1;
            for (ExamBlueprintItem bp : prompt.getBlueprintItems()) {
                int count = bp.getCount() != null ? bp.getCount() : 1;
                for (int i = 0; i < count; i++) {
                    result.add(generateTopicAwareDraft(
                            bp.getTopic() != null ? bp.getTopic() : prompt.getTopic(),
                            prompt.getSubjectName(),
                            prompt.getGradeLevel(),
                            bp.getQuestionType() != null ? bp.getQuestionType() : QuestionType.MULTIPLE_CHOICE,
                            bp.getDifficulty() != null ? bp.getDifficulty() : QuestionDifficulty.MEDIUM,
                            bp.getMarksPerQuestion() != null ? bp.getMarksPerQuestion() : BigDecimal.ONE,
                            qIndex++
                    ));
                }
            }
        } else {
            int count = prompt.getCount() != null && prompt.getCount() > 0 ? prompt.getCount() : 5;
            List<QuestionType> allowedTypes;
            if (prompt.getQuestionTypes() != null && !prompt.getQuestionTypes().isEmpty()) {
                allowedTypes = prompt.getQuestionTypes();
            } else if (prompt.getQuestionType() != null) {
                allowedTypes = List.of(prompt.getQuestionType());
            } else {
                allowedTypes = List.of(
                        QuestionType.MULTIPLE_CHOICE,
                        QuestionType.TRUE_FALSE,
                        QuestionType.FILL_IN_THE_BLANK,
                        QuestionType.SHORT_ANSWER,
                        QuestionType.ESSAY
                );
            }
            QuestionDifficulty diff = prompt.getDifficulty() != null ? prompt.getDifficulty() : QuestionDifficulty.MEDIUM;
            BigDecimal marks = prompt.getMarksPerQuestion() != null ? prompt.getMarksPerQuestion() : BigDecimal.ONE;

            for (int i = 1; i <= count; i++) {
                QuestionType currentType = allowedTypes.get((i - 1) % allowedTypes.size());
                result.add(generateTopicAwareDraft(
                        prompt.getTopic(),
                        prompt.getSubjectName(),
                        prompt.getGradeLevel(),
                        currentType,
                        diff,
                        marks,
                        i
                ));
            }
        }

        return result;
    }

    private GeneratedQuestionDraft generateTopicAwareDraft(
            String topic,
            String subjectName,
            String gradeLevel,
            QuestionType qType,
            QuestionDifficulty difficulty,
            BigDecimal marks,
            int index
    ) {
        String topicStr = (topic != null && !topic.isBlank()) ? topic : "Kiến thức cơ bản";
        String subjStr = (subjectName != null && !subjectName.isBlank()) ? subjectName : "Toán học";
        String lowerTopic = topicStr.toLowerCase();
        String lowerSubj = subjStr.toLowerCase();

        // Check if math question
        if (lowerSubj.contains("toán") || lowerTopic.contains("cộng") || lowerTopic.contains("trừ") || lowerTopic.contains("toán") || lowerTopic.contains("phạm vi 10")) {
            return generateMathQuestion(topicStr, gradeLevel, qType, difficulty, marks, index);
        }

        // Default Subject-Aware Template
        return generateGeneralSubjectQuestion(topicStr, subjStr, gradeLevel, qType, difficulty, marks, index);
    }

    private GeneratedQuestionDraft generateMathQuestion(
            String topic, String gradeLevel, QuestionType qType, QuestionDifficulty difficulty, BigDecimal marks, int index
    ) {
        int max = topic.toLowerCase().contains("10") ? 10 : (topic.toLowerCase().contains("20") ? 20 : 100);
        int a = random.nextInt(Math.min(max - 2, 7)) + 1;
        int b = random.nextInt(Math.min(max - a, 8)) + 1;
        boolean isAdd = (index % 2 == 1);
        int correct = isAdd ? (a + b) : Math.max(a, b);
        int first = isAdd ? a : Math.max(a, b);
        int second = isAdd ? b : Math.min(a, b);
        int ans = isAdd ? (first + second) : (first - second);

        String op = isAdd ? "+" : "-";
        String content;
        String explanation;
        List<GeneratedOptionDraft> options = new ArrayList<>();

        if (qType == QuestionType.MULTIPLE_CHOICE) {
            content = String.format("Câu hỏi %d: Kết quả của phép tính %d %s %d là bao nhiêu?", index, first, op, second);
            explanation = String.format("Lời giải: Ta thực hiện phép tính %d %s %d = %d. Do đó đáp án chính xác là %d.", first, op, second, ans, ans);

            options.add(GeneratedOptionDraft.builder().optionKey("A").optionText(String.valueOf(ans)).isCorrect(true).displayOrder(1).build());
            options.add(GeneratedOptionDraft.builder().optionKey("B").optionText(String.valueOf(ans + 1)).isCorrect(false).displayOrder(2).build());
            options.add(GeneratedOptionDraft.builder().optionKey("C").optionText(String.valueOf(Math.max(0, ans - 1))).isCorrect(false).displayOrder(3).build());
            options.add(GeneratedOptionDraft.builder().optionKey("D").optionText(String.valueOf(ans + 2)).isCorrect(false).displayOrder(4).build());
        } else if (qType == QuestionType.TRUE_FALSE) {
            boolean statementCorrect = random.nextBoolean();
            int shownAns = statementCorrect ? ans : (ans + 1);
            content = String.format("Câu hỏi %d (Đúng/Sai): Kết quả của phép tính %d %s %d = %d.", index, first, op, second, shownAns);
            explanation = String.format("Lời giải: Phép tính %d %s %d có kết quả chính xác là %d. Nhận định này là %s.", first, op, second, ans, statementCorrect ? "ĐÚNG" : "SAI");

            options.add(GeneratedOptionDraft.builder().optionKey("A").optionText("Đúng").isCorrect(statementCorrect).displayOrder(1).build());
            options.add(GeneratedOptionDraft.builder().optionKey("B").optionText("Sai").isCorrect(!statementCorrect).displayOrder(2).build());
        } else if (qType == QuestionType.ESSAY) {
            content = String.format("Câu hỏi %d (Tự luận): Một bạn học sinh có %d viên bi, bạn được tặng thêm %d viên bi nữa. Em hãy trình bày lời giải chi tiết để tính tổng số viên bi bạn học sinh đó có.", index, first, second);
            explanation = String.format("Lời giải chi tiết và biểu điểm:\n- Lời giải phép tính: 0.25đ\n- Phép tính: %d + %d = %d (viên bi): 0.5đ\n- Đáp số: %d viên bi: 0.25đ", first, second, first + second, first + second);
        } else {
            content = String.format("Câu hỏi %d: Điền số thích hợp vào chỗ chấm: %d %s %d = ...", index, first, op, second);
            explanation = String.format("Lời giải: %d %s %d = %d", first, op, second, ans);
            options.add(GeneratedOptionDraft.builder().optionKey("ANS").optionText(String.valueOf(ans)).isCorrect(true).displayOrder(1).build());
        }

        return GeneratedQuestionDraft.builder()
                .content(content)
                .questionType(qType)
                .difficulty(difficulty)
                .defaultMarks(marks != null ? marks : BigDecimal.ONE)
                .explanation(explanation)
                .tags(String.format("Toán học, %s, Lớp 1", topic))
                .options(options)
                .build();
    }

    private GeneratedQuestionDraft generateGeneralSubjectQuestion(
            String topicStr, String subjStr, String gradeStr, QuestionType qType, QuestionDifficulty difficulty, BigDecimal marks, int index
    ) {
        String content = String.format("Câu hỏi %d: Trong bài học về '%s' (%s - %s), nội dung trọng tâm cần ghi nhớ là gì?", index, topicStr, subjStr, gradeStr != null ? gradeStr : "");
        String explanation = String.format("Lời giải chi tiết: Khái niệm cốt lõi của bài học '%s' giúp học sinh nắm vững kiến thức nền tảng.", topicStr);
        List<GeneratedOptionDraft> options = new ArrayList<>();

        if (qType == QuestionType.MULTIPLE_CHOICE) {
            options.add(GeneratedOptionDraft.builder().optionKey("A").optionText(String.format("Nắm vững định nghĩa và ví dụ thực hành về %s", topicStr)).isCorrect(true).displayOrder(1).build());
            options.add(GeneratedOptionDraft.builder().optionKey("B").optionText("Chỉ cần học thuộc lý thuyết mà không cần làm bài tập").isCorrect(false).displayOrder(2).build());
            options.add(GeneratedOptionDraft.builder().optionKey("C").optionText("Bỏ qua các bài toán thực hành cơ bản").isCorrect(false).displayOrder(3).build());
            options.add(GeneratedOptionDraft.builder().optionKey("D").optionText("Không có đáp án nào nêu trên phù hợp").isCorrect(false).displayOrder(4).build());
        } else if (qType == QuestionType.TRUE_FALSE) {
            content = String.format("Câu hỏi %d (Đúng/Sai): Bài học '%s' là nội dung kiến thức cơ bản trong chương trình môn %s.", index, topicStr, subjStr);
            options.add(GeneratedOptionDraft.builder().optionKey("A").optionText("Đúng").isCorrect(true).displayOrder(1).build());
            options.add(GeneratedOptionDraft.builder().optionKey("B").optionText("Sai").isCorrect(false).displayOrder(2).build());
        } else if (qType == QuestionType.ESSAY) {
            content = String.format("Câu hỏi %d (Tự luận): Em hãy phân tích và trình bày hiểu biết của mình về chủ đề '%s' trong bài học môn %s.", index, topicStr, subjStr);
            explanation = String.format("Hướng dẫn chấm tự luận: Nêu được định nghĩa cốt lõi của '%s', đưa ra ít nhất một ví dụ liên hệ thực tế minh họa rõ ràng và rút ra bài học nhận thức.", topicStr);
        } else {
            content = String.format("Câu hỏi %d: Hãy nêu từ khóa quan trọng nhất trong chủ đề '%s'.", index, topicStr);
            options.add(GeneratedOptionDraft.builder().optionKey("ANS").optionText(topicStr).isCorrect(true).displayOrder(1).build());
        }

        return GeneratedQuestionDraft.builder()
                .content(content)
                .questionType(qType)
                .difficulty(difficulty)
                .defaultMarks(marks != null ? marks : BigDecimal.ONE)
                .explanation(explanation)
                .tags(String.format("%s, %s", subjStr, topicStr))
                .options(options)
                .build();
    }
}
