package com.nqd.nqd_lms_be.controller.student;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.student.*;
import com.nqd.nqd_lms_be.service.student.StudentAiTutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/ai-tutor")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
@Tag(name = "Student - AI Tutor", description = "Endpoints for students to interact with AI learning assistant and manage chat conversations")
public class StudentAiTutorController {

    private final StudentAiTutorService studentAiTutorService;

    @GetMapping("/conversations")
    @Operation(summary = "List all active chat conversations for the current student")
    public ResponseEntity<List<AiTutorConversationDto>> getConversations(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentAiTutorService.getConversations(principal.getId()));
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Get conversation details and full message history")
    public ResponseEntity<AiTutorConversationDetailDto> getConversationDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentAiTutorService.getConversationDetail(id, principal.getId()));
    }

    @PostMapping("/conversations")
    @Operation(summary = "Create a new AI Tutor chat conversation")
    public ResponseEntity<AiTutorConversationDto> createConversation(
            @Valid @RequestBody CreateAiTutorConversationRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentAiTutorService.createConversation(request, principal.getId()));
    }

    @PatchMapping("/conversations/{id}/rename")
    @Operation(summary = "Rename a chat conversation")
    public ResponseEntity<AiTutorConversationDto> renameConversation(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        String newTitle = body.getOrDefault("title", "Cuộc trò chuyện");
        return ResponseEntity.ok(studentAiTutorService.renameConversation(id, newTitle, principal.getId()));
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "Delete (soft delete) a chat conversation")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        studentAiTutorService.deleteConversation(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ask")
    @Operation(summary = "Ask a question to AI Tutor (General / Contextual QA)")
    public ResponseEntity<StudentAiTutorResponse> askAiTutor(
            @Valid @RequestBody StudentAiTutorRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentAiTutorService.askAiTutor(request, principal.getId()));
    }

    @PostMapping("/explain-lesson/{lessonId}")
    @Operation(summary = "Ask AI Tutor to explain a published lesson")
    public ResponseEntity<StudentAiTutorResponse> explainLesson(
            @PathVariable UUID lessonId,
            @RequestParam(required = false) String customQuestion,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentAiTutorService.explainLesson(lessonId, customQuestion, principal.getId()));
    }

    @PostMapping("/explain-answer")
    @Operation(summary = "Ask AI Tutor why a submitted exam answer was incorrect")
    public ResponseEntity<StudentAiTutorResponse> explainWrongAnswer(
            @RequestParam UUID examAttemptId,
            @RequestParam UUID questionId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentAiTutorService.explainWrongAnswer(examAttemptId, questionId, principal.getId()));
    }

    @PostMapping("/hint")
    @Operation(summary = "Ask AI Tutor for a Socratic hint without revealing answers")
    public ResponseEntity<StudentAiTutorResponse> provideHint(
            @RequestParam UUID examAttemptId,
            @RequestParam UUID questionId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentAiTutorService.provideHint(examAttemptId, questionId, principal.getId()));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get personalized AI study recommendations based on progress and exam results")
    public ResponseEntity<StudentAiTutorResponse> getStudyRecommendations(
            @RequestParam(required = false) UUID courseId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentAiTutorService.getStudyRecommendations(courseId, principal.getId()));
    }
}
