package com.nqd.nqd_lms_be.controller.student;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.student.*;
import com.nqd.nqd_lms_be.dto.teacher.TeacherExamResponse;
import com.nqd.nqd_lms_be.service.student.StudentExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/exams")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
@Tag(name = "Student - Exams", description = "Endpoints for students to take exams and submit answers")
public class StudentExamController {

    private final StudentExamService studentExamService;

    @GetMapping("/available")
    @Operation(summary = "Get published exams available to students")
    public ResponseEntity<List<TeacherExamResponse>> getAvailableExams() {
        return ResponseEntity.ok(studentExamService.getAvailableExams());
    }

    @GetMapping("/my-assigned-exams")
    @Operation(summary = "Get exams assigned to the current student with completion status")
    public ResponseEntity<List<StudentAssignedExamResponse>> getMyAssignedExams(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExamService.getMyAssignedExams(principal.getId()));
    }

    @PostMapping("/{examId}/start")
    @Operation(summary = "Start an exam attempt (Sanitized DTO - No answer keys)")
    public ResponseEntity<StudentExamTakingResponse> startExam(
            @PathVariable UUID examId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExamService.startExam(examId, principal.getId()));
    }

    @PostMapping("/attempts/{attemptId}/events")
    @Operation(summary = "Batch record exam attempt proctoring events (Tab blur, Fullscreen exit, Copy/Paste)")
    public ResponseEntity<ExamAttemptEventBatchResponse> recordAttemptEvents(
            @PathVariable UUID attemptId,
            @RequestBody List<ExamAttemptEventRequest> requests,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExamService.recordAttemptEvents(attemptId, requests, principal.getId()));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @Operation(summary = "Submit exam attempt answers (Ownership check enforced)")
    public ResponseEntity<StudentAttemptResultResponse> submitExam(
            @PathVariable UUID attemptId,
            @RequestBody SubmitExamAttemptRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExamService.submitExam(attemptId, request, principal.getId()));
    }

    @GetMapping("/attempts/{attemptId}/result")
    @Operation(summary = "Get exam attempt score and result (Ownership check enforced)")
    public ResponseEntity<StudentAttemptResultResponse> getAttemptResult(
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExamService.getAttemptResult(attemptId, principal.getId()));
    }

    @GetMapping("/{examId}/my-attempts")
    @Operation(summary = "Get all attempts made by current student for an exam")
    public ResponseEntity<List<StudentAttemptResultResponse>> getMyExamAttempts(
            @PathVariable UUID examId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExamService.getMyExamAttempts(examId, principal.getId()));
    }

    @GetMapping("/attempts/{attemptId}/review")
    @Operation(summary = "Review detailed right/wrong answers and explanations for an attempt (Ownership check enforced)")
    public ResponseEntity<StudentExamAttemptReviewResponse> getAttemptReview(
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExamService.getAttemptReview(attemptId, principal.getId()));
    }

    @GetMapping("/progress")
    @Operation(summary = "Get current student learning and exam progress")
    public ResponseEntity<StudentProgressResponse> getMyProgress(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentExamService.getStudentProgress(principal.getId()));
    }
}
