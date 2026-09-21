package com.nqd.nqd_lms_be.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedOptionDraft {
    private String optionKey;
    private String optionText;
    @Builder.Default
    private Boolean isCorrect = false;
    private Integer displayOrder;
}
