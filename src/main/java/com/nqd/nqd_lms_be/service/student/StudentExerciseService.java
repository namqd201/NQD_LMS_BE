package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.dto.exercise.*;

import java.util.List;
import java.util.UUID;

public interface StudentExerciseService {
    List<StudentExerciseSummaryResponse> getExercisesByLesson(UUID lessonId, UUID studentId);

    StudentExerciseSummaryResponse getExerciseById(UUID exerciseId, UUID studentId);

    StudentExerciseTakingResponse startExercise(UUID exerciseId, UUID studentId);

    StudentExerciseQuestionResultResponse submitQuestion(
            UUID attemptId,
            StudentExerciseSubmitQuestionRequest request,
            UUID studentId
    );

    StudentExerciseAttemptResultResponse submitAttempt(UUID attemptId, UUID studentId);

    StudentExerciseAttemptResultResponse getAttemptResult(UUID attemptId, UUID studentId);

    List<StudentExerciseAttemptResultResponse> getMyAttempts(UUID exerciseId, UUID studentId);

    StudentExerciseAiExplainResponse explainQuestionWithAi(
            UUID attemptId,
            UUID questionId,
            String customPrompt,
            UUID studentId
    );
}
