package com.nqd.nqd_lms_be.controller;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.RejectTeacherApplicationRequest;
import com.nqd.nqd_lms_be.dto.TeacherApplicationResponse;
import com.nqd.nqd_lms_be.entity.enums.TeacherApplicantType;
import com.nqd.nqd_lms_be.entity.enums.TeacherApplicationStatus;
import com.nqd.nqd_lms_be.service.teacher.TeacherApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/teacher-applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('ROLE_ADMIN') or hasAuthority('ROLE_ADMIN') or hasAuthority('ADMIN')")
@Tag(name = "Admin Teacher Applications", description = "Endpoints for Admins to review and approve/reject teacher verification applications")
@Slf4j
public class AdminTeacherApplicationController {

    private final TeacherApplicationService applicationService;

    @GetMapping
    @Operation(summary = "Get list of teacher applications with filters")
    public ResponseEntity<Page<TeacherApplicationResponse>> getApplications(
            @RequestParam(value = "status", required = false) TeacherApplicationStatus status,
            @RequestParam(value = "applicantType", required = false) TeacherApplicantType applicantType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Admin querying teacher applications: status={}, applicantType={}, keyword={}, pageable={}",
                status, applicantType, keyword, pageable);
        Page<TeacherApplicationResponse> result = applicationService.getApplications(status, applicantType, keyword, pageable);
        log.info("Admin query returned {} applications (total: {})", result.getNumberOfElements(), result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/pending-count")
    @Operation(summary = "Get total count of pending teacher applications")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        long count = applicationService.countPendingApplications();
        return ResponseEntity.ok(Map.of("pendingCount", count));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed teacher application by ID")
    public ResponseEntity<TeacherApplicationResponse> getApplicationById(@PathVariable UUID id) {
        TeacherApplicationResponse response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve teacher application and automatically grant ROLE_TEACHER")
    public ResponseEntity<TeacherApplicationResponse> approveApplication(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        TeacherApplicationResponse response = applicationService.approveApplication(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject teacher application with explanation reason")
    public ResponseEntity<TeacherApplicationResponse> rejectApplication(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody RejectTeacherApplicationRequest request) {
        TeacherApplicationResponse response = applicationService.rejectApplication(principal.getId(), id, request);
        return ResponseEntity.ok(response);
    }
}
