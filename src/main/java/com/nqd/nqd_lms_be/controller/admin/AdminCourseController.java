package com.nqd.nqd_lms_be.controller.admin;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.admin.AdminCourseDisableRequest;
import com.nqd.nqd_lms_be.dto.admin.AdminCourseResponse;
import com.nqd.nqd_lms_be.service.admin.AdminCourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Courses", description = "Endpoints for system-wide course inspection and moderation")
public class AdminCourseController {

    private final AdminCourseService adminCourseService;
    private final com.nqd.nqd_lms_be.service.course.CourseWorkflowService courseWorkflowService;
    private final com.nqd.nqd_lms_be.repository.CourseRepository courseRepository;

    @GetMapping("/pending-review")
    @Operation(summary = "Get all courses pending admin review for marketplace publication")
    public ResponseEntity<List<AdminCourseResponse>> getPendingReviewCourses() {
        return ResponseEntity.ok(adminCourseService.getAllCourses().stream()
                .filter(c -> c.getStatus() == com.nqd.nqd_lms_be.entity.enums.CourseStatus.PENDING_REVIEW)
                .toList());
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve course to publish on marketplace")
    public ResponseEntity<com.nqd.nqd_lms_be.dto.teacher.TeacherCourseResponse> approveCourse(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(courseWorkflowService.approveCourse(id, principal.getId()));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject course publication with explanation reason")
    public ResponseEntity<com.nqd.nqd_lms_be.dto.teacher.TeacherCourseResponse> rejectCourse(
            @PathVariable UUID id,
            @RequestBody(required = false) com.nqd.nqd_lms_be.dto.marketplace.CourseWorkflowActionRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(courseWorkflowService.rejectCourse(id, principal.getId(), reason));
    }

    @GetMapping
    @Operation(summary = "Get all courses in the system")
    public ResponseEntity<List<AdminCourseResponse>> getAllCourses() {
        return ResponseEntity.ok(adminCourseService.getAllCourses());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get course by ID")
    public ResponseEntity<AdminCourseResponse> getCourseById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminCourseService.getCourseById(id));
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "Disable/moderate a course with an explanation reason")
    public ResponseEntity<AdminCourseResponse> disableCourse(
            @PathVariable UUID id,
            @Valid @RequestBody AdminCourseDisableRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(adminCourseService.disableCourse(id, request, principal.getId()));
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "Re-enable a disabled course")
    public ResponseEntity<AdminCourseResponse> enableCourse(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(adminCourseService.enableCourse(id, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete course (Only creator admin permitted, and no enrolled students)")
    public ResponseEntity<MessageResponse> deleteCourse(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        adminCourseService.deleteCourse(id, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Course deleted successfully"));
    }
}
