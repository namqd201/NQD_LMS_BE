package com.nqd.nqd_lms_be.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Option DTO presented to students during an exam attempt.
 * Strictly DOES NOT contain `isCorrect` to prevent data leakage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentOptionTakingResponse {
    private UUID id;
    private String optionKey;
    private String optionText;
    private Integer displayOrder;
}
