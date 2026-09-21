package com.nqd.nqd_lms_be.ai.validator;

import com.nqd.nqd_lms_be.ai.dto.GeneratedOptionDraft;
import com.nqd.nqd_lms_be.ai.dto.GeneratedQuestionDraft;
import com.nqd.nqd_lms_be.entity.enums.AiValidationStatus;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AiQuestionValidator {

    public record ValidationResult(
            boolean isValid,
            AiValidationStatus status,
            String feedback
    ) {}

    public ValidationResult validate(GeneratedQuestionDraft draft) {
        if (draft == null) {
            return new ValidationResult(false, AiValidationStatus.INVALID, "Dữ liệu câu hỏi bị rỗng.");
        }

        // 1. Content check
        if (draft.getContent() == null || draft.getContent().trim().length() < 5) {
            return new ValidationResult(false, AiValidationStatus.INVALID, "Nội dung câu hỏi quá ngắn hoặc bị trống (tối thiểu 5 ký tự).");
        }

        // 2. Type & difficulty check
        if (draft.getQuestionType() == null) {
            return new ValidationResult(false, AiValidationStatus.INVALID, "Loại câu hỏi không được để trống.");
        }
        if (draft.getDifficulty() == null) {
            return new ValidationResult(false, AiValidationStatus.INVALID, "Độ khó câu hỏi không được để trống.");
        }

        // 3. Marks check
        if (draft.getDefaultMarks() == null || draft.getDefaultMarks().compareTo(BigDecimal.ZERO) <= 0) {
            return new ValidationResult(false, AiValidationStatus.WARNING, "Điểm câu hỏi phải lớn hơn 0.");
        }

        List<GeneratedOptionDraft> options = draft.getOptions();

        // 4. Type-specific checks
        switch (draft.getQuestionType()) {
            case MULTIPLE_CHOICE -> {
                if (options == null || options.size() < 2) {
                    return new ValidationResult(false, AiValidationStatus.INVALID, "Câu hỏi trắc nghiệm phải có ít nhất 2 đáp án lựa chọn.");
                }

                Set<String> keys = new HashSet<>();
                long correctCount = 0;

                for (GeneratedOptionDraft opt : options) {
                    if (opt.getOptionKey() == null || opt.getOptionKey().trim().isEmpty()) {
                        return new ValidationResult(false, AiValidationStatus.INVALID, "Mỗi lựa chọn phải có ký tự định danh (A, B, C, D...).");
                    }
                    String key = opt.getOptionKey().trim().toUpperCase();
                    if (keys.contains(key)) {
                        return new ValidationResult(false, AiValidationStatus.INVALID, "Trùng lặp mã lựa chọn: " + key);
                    }
                    keys.add(key);

                    if (opt.getOptionText() == null || opt.getOptionText().trim().isEmpty()) {
                        return new ValidationResult(false, AiValidationStatus.INVALID, "Nội dung đáp án " + key + " không được để trống.");
                    }

                    if (Boolean.TRUE.equals(opt.getIsCorrect())) {
                        correctCount++;
                    }
                }

                if (correctCount == 0) {
                    return new ValidationResult(false, AiValidationStatus.INVALID, "Chưa thiết lập đáp án đúng nào cho câu hỏi trắc nghiệm.");
                }
            }

            case TRUE_FALSE -> {
                if (options == null || options.size() != 2) {
                    return new ValidationResult(false, AiValidationStatus.INVALID, "Câu hỏi Đúng/Sai phải có chính xác 2 lựa chọn (Đúng và Sai).");
                }
                long correctCount = options.stream().filter(o -> Boolean.TRUE.equals(o.getIsCorrect())).count();
                if (correctCount != 1) {
                    return new ValidationResult(false, AiValidationStatus.INVALID, "Câu hỏi Đúng/Sai phải có đúng 1 đáp án chính xác.");
                }
            }

            case SHORT_ANSWER, FILL_IN_THE_BLANK -> {
                if (options == null || options.isEmpty()) {
                    return new ValidationResult(false, AiValidationStatus.INVALID, "Câu hỏi điền từ / trả lời ngắn phải có ít nhất 1 đáp án mẫu chính xác.");
                }
                boolean hasCorrect = options.stream().anyMatch(o -> o.getOptionText() != null && !o.getOptionText().trim().isEmpty());
                if (!hasCorrect) {
                    return new ValidationResult(false, AiValidationStatus.INVALID, "Chưa điền nội dung đáp án chính xác.");
                }
            }

            case ESSAY -> {
                // Essay requires question content, explanation optional
                if (draft.getContent().trim().length() < 10) {
                    return new ValidationResult(false, AiValidationStatus.WARNING, "Đề bài tự luận nên có mô tả chi tiết hơn.");
                }
            }
        }

        return new ValidationResult(true, AiValidationStatus.VALID, "Dữ liệu câu hỏi hợp lệ.");
    }
}
