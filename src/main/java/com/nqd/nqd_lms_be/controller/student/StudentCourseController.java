package com.nqd.nqd_lms_be.controller.student;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.student.StudentCourseResponse;
import com.nqd.nqd_lms_be.service.student.StudentCourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/courses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
@Tag(name = "Student - Courses", description = "Endpoints for students to browse and enroll in courses")
public class StudentCourseController {

    private final StudentCourseService studentCourseService;

    @GetMapping("/published")
    @Operation(summary = "Get list of published learning courses")
    public ResponseEntity<List<StudentCourseResponse>> getPublishedCourses(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentCourseService.getPublishedCourses(principal.getId()));
    }

    @GetMapping("/enrolled")
    @Operation(summary = "Get courses the current student is enrolled in")
    public ResponseEntity<List<StudentCourseResponse>> getEnrolledCourses(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentCourseService.getEnrolledCourses(principal.getId()));
    }

    @PostMapping("/{courseId}/enroll")
    @Operation(summary = "Enroll current student into an active course")
    public ResponseEntity<MessageResponse> enrollCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        studentCourseService.enrollCourse(courseId, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Enrolled successfully in course"));
    }
}
