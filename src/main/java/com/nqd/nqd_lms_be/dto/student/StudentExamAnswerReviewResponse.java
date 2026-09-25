package com.nqd.nqd_lms_be.dto.student;

import com.nqd.nqd_lms_be.dto.teacher.TeacherQuestionOptionDto;
import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExamAnswerReviewResponse {
    private UUID questionId;
    private Integer displayOrder;
    private String content;
    private QuestionType questionType;
    private QuestionDifficulty difficulty;
    private BigDecimal maxMarks;
    private BigDecimal marksAwarded;
    private Boolean isCorrect;
    private UUID studentSelectedOptionId;
    private String studentSelectedOptionKey;
    private String studentSelectedOptionText;
    private String studentAnswerText;
    private String correctOptionKey;
    private String correctOptionText;
    private String explanation;
    private String audioUrl;
    private String audioScript;
    private List<TeacherQuestionOptionDto> options;
}
