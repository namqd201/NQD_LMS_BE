package com.nqd.nqd_lms_be.controller.teacher;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.freegrant.*;
import com.nqd.nqd_lms_be.service.freegrant.CourseFreeGrantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/courses/{courseId}/free-grants")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher - Free Grants", description = "Endpoints for course free student grants and quota management")
public class TeacherCourseFreeGrantController {

    private final CourseFreeGrantService courseFreeGrantService;

    @GetMapping
    @Operation(summary = "Get free grants summary, remaining quota and granted students for a course")
    public ResponseEntity<CourseFreeGrantSummaryResponse> getSummary(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(courseFreeGrantService.getFreeGrantSummary(courseId, principal.getId()));
    }

    @PostMapping
    @Operation(summary = "Grant free access to a student by email")
    public ResponseEntity<TeacherFreeGrantResponse> addFreeGrant(
            @PathVariable UUID courseId,
            @Valid @RequestBody AddFreeGrantRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseFreeGrantService.addFreeGrant(courseId, principal.getId(), request));
    }

    @PostMapping("/request-quota")
    @Operation(summary = "Request additional free grants quota from admin")
    public ResponseEntity<CourseFreeQuotaRequestResponse> requestQuota(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateQuotaRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseFreeGrantService.requestQuota(courseId, principal.getId(), request));
    }
}
