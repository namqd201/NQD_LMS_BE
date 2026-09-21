package com.nqd.nqd_lms_be.controller.admin;

import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.admin.SubjectRequest;
import com.nqd.nqd_lms_be.dto.admin.SubjectResponse;
import com.nqd.nqd_lms_be.entity.enums.SubjectStatus;
import com.nqd.nqd_lms_be.service.admin.AdminSubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/subjects")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Subject Management", description = "Endpoints for administrators to manage academic subjects")
public class AdminSubjectController {

    private final AdminSubjectService adminSubjectService;

    @GetMapping
    @Operation(summary = "Get all subjects with course count")
    public ResponseEntity<List<SubjectResponse>> getAllSubjects() {
        return ResponseEntity.ok(adminSubjectService.getAllSubjects());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subject by ID")
    public ResponseEntity<SubjectResponse> getSubjectById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminSubjectService.getSubjectById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new subject")
    public ResponseEntity<SubjectResponse> createSubject(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminSubjectService.createSubject(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update subject information")
    public ResponseEntity<SubjectResponse> updateSubject(
            @PathVariable UUID id,
            @Valid @RequestBody SubjectRequest request
    ) {
        return ResponseEntity.ok(adminSubjectService.updateSubject(id, request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update subject status (ACTIVE / INACTIVE)")
    public ResponseEntity<SubjectResponse> updateSubjectStatus(
            @PathVariable UUID id,
            @RequestParam SubjectStatus status
    ) {
        return ResponseEntity.ok(adminSubjectService.updateSubjectStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete subject (Soft delete)")
    public ResponseEntity<MessageResponse> deleteSubject(@PathVariable UUID id) {
        adminSubjectService.deleteSubject(id);
        return ResponseEntity.ok(MessageResponse.of("Đã chuyển môn học vào thùng rác"));
    }

    @GetMapping("/trash")
    @Operation(summary = "Get deleted subjects history / trash")
    public ResponseEntity<List<SubjectResponse>> getDeletedSubjects() {
        return ResponseEntity.ok(adminSubjectService.getDeletedSubjects());
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "Restore a soft-deleted subject")
    public ResponseEntity<SubjectResponse> restoreSubject(@PathVariable UUID id) {
        return ResponseEntity.ok(adminSubjectService.restoreSubject(id));
    }
}
