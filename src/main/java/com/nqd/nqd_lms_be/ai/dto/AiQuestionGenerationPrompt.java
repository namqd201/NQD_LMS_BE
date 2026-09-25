package com.nqd.nqd_lms_be.ai.dto;

import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiQuestionGenerationPrompt {
    private String subjectName;
    private String courseName;
    private String lessonName;
    private String gradeLevel;
    private String topic;
    private QuestionType questionType;
    private List<QuestionType> questionTypes;
    private QuestionDifficulty difficulty;
    @Builder.Default
    private Integer count = 5;
    @Builder.Default
    private BigDecimal marksPerQuestion = new BigDecimal("1.00");
    private String additionalInstructions;
    @Builder.Default
    private List<ExamBlueprintItem> blueprintItems = new ArrayList<>();
    @Builder.Default
    private Boolean isExamBlueprint = false;
    @Builder.Default
    private Boolean isListening = false;
    private String listeningPassageType;
}
