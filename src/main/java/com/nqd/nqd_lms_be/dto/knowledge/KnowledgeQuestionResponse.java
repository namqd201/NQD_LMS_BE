package com.nqd.nqd_lms_be.dto.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeQuestionResponse {
    private UUID id;
    private Integer questionOrder;
    private String questionText;
    private List<String> options;
    private String correctAnswer;
    private String explanation;
}
