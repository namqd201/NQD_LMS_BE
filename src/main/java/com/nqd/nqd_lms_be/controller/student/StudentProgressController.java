package com.nqd.nqd_lms_be.controller.student;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.student.StudentCourseProgressResponse;
import com.nqd.nqd_lms_be.dto.student.StudentLessonProgressResponse;
import com.nqd.nqd_lms_be.dto.student.UpdateLessonProgressRequest;
import com.nqd.nqd_lms_be.service.student.StudentProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
@Tag(name = "Student - Learning Progress", description = "Endpoints for students to track and update their own learning progress")
public class StudentProgressController {

    private final StudentProgressService studentProgressService;

    @GetMapping("/lessons/{lessonId}/progress")
    @Operation(summary = "Get current authenticated student's progress on a lesson")
    public ResponseEntity<StudentLessonProgressResponse> getLessonProgress(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentProgressService.getLessonProgress(principal.getId(), lessonId));
    }

    @PutMapping("/lessons/{lessonId}/progress")
    @Operation(summary = "Update current authenticated student's progress on a lesson (Identity enforced via SecurityContext)")
    public ResponseEntity<StudentLessonProgressResponse> updateLessonProgress(
            @PathVariable UUID lessonId,
            @Valid @RequestBody UpdateLessonProgressRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentProgressService.updateLessonProgress(principal.getId(), lessonId, request));
    }

    @GetMapping("/courses/{courseId}/progress")
    @Operation(summary = "Get current authenticated student's overall progress on an active course")
    public ResponseEntity<StudentCourseProgressResponse> getCourseProgress(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentProgressService.getCourseProgress(principal.getId(), courseId));
    }

    @GetMapping({"/progress/analytics", "/progress/me", "/analytics/summary"})
    @Operation(summary = "Get current authenticated student's overall learning analytics, streaks, and heatmap")
    public ResponseEntity<com.nqd.nqd_lms_be.dto.student.StudentOverallAnalyticsResponse> getStudentOverallAnalytics(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentProgressService.getStudentOverallAnalytics(principal.getId()));
    }
}
