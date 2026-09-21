package com.nqd.nqd_lms_be.controller.teacher;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.teacher.CourseAnalyticsResponse;
import com.nqd.nqd_lms_be.dto.teacher.TeacherCourseRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherCourseResponse;
import com.nqd.nqd_lms_be.service.teacher.TeacherCourseService;
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
@RequestMapping("/api/v1/teacher/courses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher - Courses", description = "Endpoints for course management by teachers")
public class TeacherCourseController {

    private final TeacherCourseService teacherCourseService;
    private final com.nqd.nqd_lms_be.service.course.CourseWorkflowService courseWorkflowService;

    @PostMapping("/{id}/submit-review")
    @Operation(summary = "Submit course for admin review to be published on marketplace")
    public ResponseEntity<TeacherCourseResponse> submitForReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(courseWorkflowService.submitForReview(id, principal.getId()));
    }

    @GetMapping
    @Operation(summary = "Get courses taught or created by current teacher")
    public ResponseEntity<List<TeacherCourseResponse>> getMyCourses(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherCourseService.getTeacherCourses(principal.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get course details by ID (Ownership check enforced)")
    public ResponseEntity<TeacherCourseResponse> getCourseById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherCourseService.getCourseById(id, principal.getId()));
    }

    @PostMapping
    @Operation(summary = "Create a new course")
    public ResponseEntity<TeacherCourseResponse> createCourse(
            @Valid @RequestBody TeacherCourseRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        TeacherCourseResponse response = teacherCourseService.createCourse(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update course (Ownership check enforced)")
    public ResponseEntity<TeacherCourseResponse> updateCourse(
            @PathVariable UUID id,
            @Valid @RequestBody TeacherCourseRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherCourseService.updateCourse(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete course (Soft delete with ownership check)")
    public ResponseEntity<MessageResponse> deleteCourse(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherCourseService.deleteCourse(id, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã chuyển khóa học vào thùng rác"));
    }

    @GetMapping("/trash")
    @Operation(summary = "Get deleted courses history / trash")
    public ResponseEntity<List<TeacherCourseResponse>> getDeletedCourses(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherCourseService.getDeletedCourses(principal.getId()));
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "Restore a soft-deleted course")
    public ResponseEntity<TeacherCourseResponse> restoreCourse(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherCourseService.restoreCourse(id, principal.getId()));
    }

    @GetMapping("/{id}/analytics")
    @Operation(summary = "Get analytics for a course (Ownership check enforced)")
    public ResponseEntity<CourseAnalyticsResponse> getCourseAnalytics(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherCourseService.getCourseAnalytics(id, principal.getId()));
    }

    @GetMapping("/{id}/enrollments")
    @Operation(summary = "Get all student enrollment requests for a course (Ownership check enforced)")
    public ResponseEntity<List<com.nqd.nqd_lms_be.dto.teacher.TeacherEnrollmentResponse>> getCourseEnrollments(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherCourseService.getCourseEnrollments(id, principal.getId()));
    }

    @PutMapping("/{id}/enrollments/{enrollmentId}/approve")
    @Operation(summary = "Approve student enrollment into private course")
    public ResponseEntity<MessageResponse> approveEnrollment(
            @PathVariable UUID id,
            @PathVariable UUID enrollmentId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherCourseService.approveEnrollment(id, enrollmentId, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Enrollment approved successfully"));
    }

    @PutMapping("/{id}/enrollments/{enrollmentId}/reject")
    @Operation(summary = "Reject student enrollment request")
    public ResponseEntity<MessageResponse> rejectEnrollment(
            @PathVariable UUID id,
            @PathVariable UUID enrollmentId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherCourseService.rejectEnrollment(id, enrollmentId, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Enrollment rejected successfully"));
    }
}
