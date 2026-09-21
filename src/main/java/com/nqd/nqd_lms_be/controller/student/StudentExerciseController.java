package com.nqd.nqd_lms_be.controller.student;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.exercise.*;
import com.nqd.nqd_lms_be.service.student.StudentExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/exercises")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
@Tag(name = "Student - Exercises", description = "Endpoints for students to practice exercises, auto-check questions, and review attempts")
public class StudentExerciseController {

    private final StudentExerciseService studentExerciseService;

    @GetMapping("/lesson/{lessonId}")
    @Operation(summary = "Get available practice exercises for a lesson")
    public ResponseEntity<List<StudentExerciseSummaryResponse>> getExercisesByLesson(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExerciseService.getExercisesByLesson(lessonId, principal.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exercise summary by ID")
    public ResponseEntity<StudentExerciseSummaryResponse> getExerciseById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExerciseService.getExerciseById(id, principal.getId()));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start or resume an exercise attempt")
    public ResponseEntity<StudentExerciseTakingResponse> startExercise(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExerciseService.startExercise(id, principal.getId()));
    }

    @PostMapping("/attempts/{attemptId}/submit-question")
    @Operation(summary = "Submit a single question and get instant auto-check result")
    public ResponseEntity<StudentExerciseQuestionResultResponse> submitQuestion(
            @PathVariable UUID attemptId,
            @Valid @RequestBody StudentExerciseSubmitQuestionRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExerciseService.submitQuestion(attemptId, request, principal.getId()));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @Operation(summary = "Finish exercise attempt and calculate final score")
    public ResponseEntity<StudentExerciseAttemptResultResponse> submitAttempt(
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExerciseService.submitAttempt(attemptId, principal.getId()));
    }

    @GetMapping("/attempts/{attemptId}/result")
    @Operation(summary = "Get detailed result and review of an attempt")
    public ResponseEntity<StudentExerciseAttemptResultResponse> getAttemptResult(
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExerciseService.getAttemptResult(attemptId, principal.getId()));
    }

    @GetMapping("/{id}/my-attempts")
    @Operation(summary = "Get all past attempts for an exercise")
    public ResponseEntity<List<StudentExerciseAttemptResultResponse>> getMyAttempts(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExerciseService.getMyAttempts(id, principal.getId()));
    }

    @PostMapping("/attempts/{attemptId}/questions/{questionId}/ai-explain")
    @Operation(summary = "Ask AI Tutor to explain why an answer is correct or provide step-by-step reasoning")
    public ResponseEntity<StudentExerciseAiExplainResponse> explainQuestionWithAi(
            @PathVariable UUID attemptId,
            @PathVariable UUID questionId,
            @RequestParam(required = false) String customPrompt,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExerciseService.explainQuestionWithAi(attemptId, questionId, customPrompt, principal.getId()));
    }
}
