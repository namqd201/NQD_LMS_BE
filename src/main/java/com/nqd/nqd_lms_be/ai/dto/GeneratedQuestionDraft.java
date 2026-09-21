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
public class GeneratedQuestionDraft {
    private String content;
    private QuestionType questionType;
    private QuestionDifficulty difficulty;
    @Builder.Default
    private BigDecimal defaultMarks = new BigDecimal("1.00");
    private String explanation;
    private String tags;
    @Builder.Default
    private List<GeneratedOptionDraft> options = new ArrayList<>();
}
