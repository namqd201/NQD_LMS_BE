package com.nqd.nqd_lms_be.dto.exercise;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherExerciseOptionResponse {
    private UUID id;
    private String optionKey;
    private String optionText;
    private Boolean isCorrect;
    private Integer displayOrder;
}
