package com.nqd.nqd_lms_be.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAiJobDetailResponse {
    private TeacherAiJobResponse job;
    @Builder.Default
    private List<TeacherAiGeneratedQuestionResponse> questions = new ArrayList<>();
}
