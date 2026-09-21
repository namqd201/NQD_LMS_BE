package com.nqd.nqd_lms_be.controller.admin;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.teacher.TeacherExamResponse;
import com.nqd.nqd_lms_be.entity.enums.ExamStatus;
import com.nqd.nqd_lms_be.entity.enums.ExamVisibility;
import com.nqd.nqd_lms_be.service.teacher.TeacherExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/exams")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Exam Management", description = "Endpoints for administrators to manage and moderate exams")
public class AdminExamController {

    private final TeacherExamService teacherExamService;

    @GetMapping
    @Operation(summary = "Get all platform exams with filters")
    public ResponseEntity<List<TeacherExamResponse>> getAllExams(
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

    @GetMapping("/{id}")
    @Operation(summary = "Get exam details by ID")
    public ResponseEntity<TeacherExamResponse> getExamById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherExamService.getExamById(id, principal.getId()));
    }

    @PatchMapping("/{id}/visibility")
    @Operation(summary = "Moderate exam visibility (PRIVATE, SUBJECT_SHARED, PUBLIC)")
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
    @Operation(summary = "Delete an exam")
    public ResponseEntity<MessageResponse> deleteExam(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherExamService.deleteExam(id, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã xóa đề thi thành công"));
    }
}
