package com.nqd.nqd_lms_be.controller.teacher;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.teacher.*;
import com.nqd.nqd_lms_be.entity.enums.ExamStatus;
import com.nqd.nqd_lms_be.entity.enums.ExamVisibility;
import com.nqd.nqd_lms_be.service.teacher.TeacherExamService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/exams")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher - Exams", description = "Endpoints for exam, blueprint, and grading management by teachers")
public class TeacherExamController {

    private final TeacherExamService teacherExamService;

    @GetMapping
    @Operation(summary = "Get exams created by current teacher with filters")
    public ResponseEntity<List<TeacherExamResponse>> getMyExams(
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) String gradeLevel,
            @RequestParam(required = false) ExamStatus status,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExamService.getExams(
                subjectId, courseId, gradeLevel, status, keyword, principal.getId()
        ));
    }

    @GetMapping("/shared-library")
    @Operation(summary = "Get shared/public exam library for teachers")
    public ResponseEntity<List<TeacherExamResponse>> getSharedExamLibrary(
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) String gradeLevel,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExamService.getSharedExamLibrary(
                subjectId, gradeLevel, keyword, principal.getId()
        ));
    }

    @PostMapping("/{id}/clone")
    @Operation(summary = "Clone an exam from shared library or own exam")
    public ResponseEntity<TeacherExamResponse> cloneExam(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherExamService.cloneExam(id, principal.getId()));
    }

    @PatchMapping("/{id}/visibility")
    @Operation(summary = "Update visibility of an exam (PRIVATE, SUBJECT_SHARED, PUBLIC)")
    public ResponseEntity<TeacherExamResponse> updateVisibility(
            @PathVariable UUID id,
            @RequestParam(required = false) ExamVisibility visibility,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        ExamVisibility effectiveVisibility = visibility;
        if (effectiveVisibility == null && body != null && body.containsKey("visibility")) {
            effectiveVisibility = ExamVisibility.valueOf(body.get("visibility").toUpperCase());
        }
        return ResponseEntity.ok(teacherExamService.updateExamVisibility(id, effectiveVisibility, principal.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exam details by ID")
    public ResponseEntity<TeacherExamResponse> getExamById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExamService.getExamById(id, principal.getId()));
    }

    @PostMapping
    @Operation(summary = "Create a new exam")
    public ResponseEntity<TeacherExamResponse> createExam(
            @Valid @RequestBody TeacherExamRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        TeacherExamResponse response = teacherExamService.createExam(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an exam (Ownership check enforced)")
    public ResponseEntity<TeacherExamResponse> updateExam(
            @PathVariable UUID id,
            @Valid @RequestBody TeacherExamRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExamService.updateExam(id, request, principal.getId()));
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "Publish an exam")
    public ResponseEntity<TeacherExamResponse> publishExam(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExamService.publishExam(id, principal.getId()));
    }

    @PutMapping("/{id}/archive")
    @Operation(summary = "Archive an exam")
    public ResponseEntity<TeacherExamResponse> archiveExam(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExamService.archiveExam(id, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an exam (Soft delete with ownership check)")
    public ResponseEntity<MessageResponse> deleteExam(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherExamService.deleteExam(id, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã chuyển đề thi vào thùng rác"));
    }

    @GetMapping("/trash")
    @Operation(summary = "Get deleted exams history / trash")
    public ResponseEntity<List<TeacherExamResponse>> getDeletedExams(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExamService.getDeletedExams(principal.getId()));
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "Restore a soft-deleted exam")
    public ResponseEntity<TeacherExamResponse> restoreExam(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExamService.restoreExam(id, principal.getId()));
    }

    @PostMapping("/{id}/blueprints")
    @Operation(summary = "Create or update exam blueprint (Ownership check enforced)")
    public ResponseEntity<TeacherBlueprintResponse> createBlueprint(
            @PathVariable UUID id,
            @Valid @RequestBody TeacherBlueprintRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        TeacherBlueprintResponse response = teacherExamService.createOrUpdateBlueprint(id, request, principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/blueprints")
    @Operation(summary = "Get exam blueprint details (Ownership check enforced)")
    public ResponseEntity<TeacherBlueprintResponse> getBlueprint(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExamService.getBlueprint(id, principal.getId()));
    }

    @PostMapping("/attempts/{attemptId}/grade")
    @Operation(summary = "Grade a student exam attempt (Ownership check enforced)")
    public ResponseEntity<MessageResponse> gradeAttempt(
            @PathVariable UUID attemptId,
            @Valid @RequestBody TeacherGradeAttemptRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherExamService.gradeAttempt(attemptId, request, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Attempt graded successfully"));
    }

    @GetMapping("/{id}/eligible-students")
    @Operation(summary = "Get list of students eligible to take this exam")
    public ResponseEntity<List<TeacherExamStudentCandidateResponse>> getEligibleStudents(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExamService.getEligibleStudentsForExam(id, principal.getId()));
    }

    @PostMapping("/{id}/assign-students")
    @Operation(summary = "Assign or update students participating in this exam (sends notifications)")
    public ResponseEntity<MessageResponse> assignStudents(
            @PathVariable UUID id,
            @Valid @RequestBody AssignStudentsToExamRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherExamService.assignStudentsToExam(id, request, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã giao bài kiểm tra cho các học viên thành công"));
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Get aggregated student results and attempt submissions for an exam (Ownership check enforced)")
    public ResponseEntity<TeacherExamResultsSummaryResponse> getExamResults(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExamService.getExamResults(id, principal.getId()));
    }

    @GetMapping("/attempts/{attemptId}/detail")
    @Operation(summary = "Get detailed question-by-question student attempt submission (Ownership check enforced)")
    public ResponseEntity<TeacherExamAttemptDetailResponse> getAttemptDetail(
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExamService.getAttemptDetail(attemptId, principal.getId()));
    }
}
