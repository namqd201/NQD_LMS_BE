package com.nqd.nqd_lms_be.controller.teacher;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.exercise.TeacherExerciseQuestionItemRequest;
import com.nqd.nqd_lms_be.dto.exercise.TeacherExerciseRequest;
import com.nqd.nqd_lms_be.dto.exercise.TeacherExerciseResponse;
import com.nqd.nqd_lms_be.entity.enums.ExerciseStatus;
import com.nqd.nqd_lms_be.service.teacher.TeacherExerciseService;
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
@RequestMapping("/api/v1/teacher/exercises")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher - Exercises", description = "Endpoints for practice exercise management by teachers")
public class TeacherExerciseController {

    private final TeacherExerciseService teacherExerciseService;

    @GetMapping
    @Operation(summary = "Get exercises with filters")
    public ResponseEntity<List<TeacherExerciseResponse>> getExercises(
            @RequestParam(required = false) UUID lessonId,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) ExerciseStatus status,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExerciseService.getExercises(
                lessonId, courseId, subjectId, status, keyword, principal.getId()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exercise details by ID")
    public ResponseEntity<TeacherExerciseResponse> getExerciseById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExerciseService.getExerciseById(id, principal.getId()));
    }

    @PostMapping
    @Operation(summary = "Create a new exercise")
    public ResponseEntity<TeacherExerciseResponse> createExercise(
            @Valid @RequestBody TeacherExerciseRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teacherExerciseService.createExercise(request, principal.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an exercise")
    public ResponseEntity<TeacherExerciseResponse> updateExercise(
            @PathVariable UUID id,
            @Valid @RequestBody TeacherExerciseRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExerciseService.updateExercise(id, request, principal.getId()));
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "Publish an exercise")
    public ResponseEntity<TeacherExerciseResponse> publishExercise(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExerciseService.publishExercise(id, principal.getId()));
    }

    @PutMapping("/{id}/archive")
    @Operation(summary = "Archive an exercise")
    public ResponseEntity<TeacherExerciseResponse> archiveExercise(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExerciseService.archiveExercise(id, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete an exercise")
    public ResponseEntity<MessageResponse> deleteExercise(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherExerciseService.deleteExercise(id, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã xóa bài tập thành công"));
    }

    @PostMapping("/{id}/questions")
    @Operation(summary = "Add a question to exercise")
    public ResponseEntity<TeacherExerciseResponse> addQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody TeacherExerciseQuestionItemRequest itemRequest,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExerciseService.addQuestionToExercise(id, itemRequest, principal.getId()));
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    @Operation(summary = "Remove a question from exercise")
    public ResponseEntity<TeacherExerciseResponse> removeQuestion(
            @PathVariable UUID id,
            @PathVariable UUID questionId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExerciseService.removeQuestionFromExercise(id, questionId, principal.getId()));
    }
}
