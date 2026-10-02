package com.nqd.nqd_lms_be.controller.admin;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.freegrant.CourseFreeQuotaRequestResponse;
import com.nqd.nqd_lms_be.dto.freegrant.ReviewQuotaRequest;
import com.nqd.nqd_lms_be.entity.enums.FreeQuotaRequestStatus;
import com.nqd.nqd_lms_be.service.freegrant.CourseFreeGrantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/courses/quota-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Course Quota Requests", description = "Endpoints for admin to review course free grants quota requests")
public class AdminCourseFreeGrantController {

    private final CourseFreeGrantService courseFreeGrantService;

    @GetMapping
    @Operation(summary = "Get list of free grant quota requests with optional status filter")
    public ResponseEntity<Page<CourseFreeQuotaRequestResponse>> getQuotaRequests(
            @RequestParam(required = false) FreeQuotaRequestStatus status,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(courseFreeGrantService.getAdminQuotaRequests(status, pageable));
    }

    @PutMapping("/{id}/review")
    @Operation(summary = "Approve or reject a course free grant quota request")
    public ResponseEntity<CourseFreeQuotaRequestResponse> reviewQuotaRequest(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewQuotaRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        String adminEmail = principal != null && principal.getEmail() != null ? principal.getEmail() : "admin";
        return ResponseEntity.ok(courseFreeGrantService.reviewQuotaRequest(id, request, adminEmail));
    }
}
