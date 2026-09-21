package com.nqd.nqd_lms_be.controller.teacher;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.teacher.*;
import com.nqd.nqd_lms_be.service.teacher.TeacherAiGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/ai")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher - AI Question & Exam Generation", description = "AI-powered generation of structured questions and exams for teachers using LangChain4j")
public class TeacherAiGenerationController {

    private final TeacherAiGenerationService teacherAiGenerationService;

    @PostMapping("/questions/generate")
    @Operation(summary = "Generate structured questions using AI from topic/lesson/course prompt")
    public ResponseEntity<TeacherAiJobDetailResponse> generateQuestions(
            @Valid @RequestBody TeacherAiGenerateQuestionsRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teacherAiGenerationService.generateQuestions(request, principal.getId()));
    }

    @PostMapping("/exams/generate")
    @Operation(summary = "Generate an entire exam and questions using AI from blueprint")
    public ResponseEntity<TeacherAiJobDetailResponse> generateExam(
            @Valid @RequestBody TeacherAiGenerateExamRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teacherAiGenerationService.generateExam(request, principal.getId()));
    }

    @GetMapping("/jobs")
    @Operation(summary = "Get list of all AI generation jobs for the current teacher")
    public ResponseEntity<List<TeacherAiJobResponse>> getMyJobs(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherAiGenerationService.getMyJobs(principal.getId()));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get detailed AI generation job and all generated questions")
    public ResponseEntity<TeacherAiJobDetailResponse> getJobDetail(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherAiGenerationService.getJobDetail(jobId, principal.getId()));
    }

    @PutMapping("/jobs/{jobId}/questions/{questionId}")
    @Operation(summary = "Edit/update a generated question before approving")
    public ResponseEntity<TeacherAiGeneratedQuestionResponse> updateGeneratedQuestion(
            @PathVariable UUID jobId,
            @PathVariable UUID questionId,
            @Valid @RequestBody TeacherAiUpdateGeneratedQuestionRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherAiGenerationService.updateGeneratedQuestion(jobId, questionId, request, principal.getId()));
    }

    @PostMapping("/jobs/{jobId}/questions/{questionId}/approve")
    @Operation(summary = "Approve a single AI question and save it directly into Question Bank")
    public ResponseEntity<TeacherAiGeneratedQuestionResponse> approveSingleQuestion(
            @PathVariable UUID jobId,
            @PathVariable UUID questionId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherAiGenerationService.approveSingleQuestion(jobId, questionId, principal.getId()));
    }

    @PostMapping("/jobs/{jobId}/questions/{questionId}/reject")
    @Operation(summary = "Reject a single AI generated question")
    public ResponseEntity<TeacherAiGeneratedQuestionResponse> rejectSingleQuestion(
            @PathVariable UUID jobId,
            @PathVariable UUID questionId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherAiGenerationService.rejectSingleQuestion(jobId, questionId, principal.getId()));
    }

    @PostMapping("/jobs/{jobId}/approve-all")
    @Operation(summary = "Bulk-approve all valid generated questions into Question Bank")
    public ResponseEntity<TeacherAiApproveJobResponse> approveAllValidQuestions(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherAiGenerationService.approveAllValidQuestions(jobId, principal.getId()));
    }

    @PostMapping("/jobs/{jobId}/create-exam")
    @Operation(summary = "Create an Exam in draft status with approved AI questions")
    public ResponseEntity<TeacherAiApproveJobResponse> createExamFromJob(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherAiGenerationService.createExamFromJob(jobId, principal.getId()));
    }

    @DeleteMapping("/jobs/{jobId}")
    @Operation(summary = "Delete an AI generation job")
    public ResponseEntity<Void> deleteJob(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherAiGenerationService.deleteJob(jobId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
