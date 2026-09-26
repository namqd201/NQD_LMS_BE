package com.nqd.nqd_lms_be.dto.teacher;

import com.nqd.nqd_lms_be.entity.enums.AiReviewStatus;
import com.nqd.nqd_lms_be.entity.enums.AiValidationStatus;
import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAiGeneratedQuestionResponse {
    private UUID id;
    private String content;
    private QuestionType questionType;
    private QuestionDifficulty difficulty;
    private BigDecimal marks;
    private String explanation;
    private String tags;
    private String audioUrl;
    private String audioScript;
    private String imageUrl;
    private Integer displayOrder;
    private AiValidationStatus validationStatus;
    private String validationFeedback;
    private AiReviewStatus reviewStatus;
    private UUID approvedQuestionId;
    @Builder.Default
    private List<TeacherAiGeneratedOptionResponse> options = new ArrayList<>();
}
