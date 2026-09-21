package com.nqd.nqd_lms_be.controller.student;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.student.StudentResourceResponse;
import com.nqd.nqd_lms_be.service.student.StudentLessonResourceService;
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
@RequestMapping("/api/v1/student/lessons/{lessonId}/resources")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
@Tag(name = "Student - Lesson Resources", description = "Endpoints for students to view attached resources of published lessons")
public class StudentLessonResourceController {

    private final StudentLessonResourceService studentLessonResourceService;

    @GetMapping
    @Operation(summary = "Get published resources attached to a published lesson in an active course")
    public ResponseEntity<List<StudentResourceResponse>> getLessonResources(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentLessonResourceService.getLessonResources(principal.getId(), lessonId));
    }
}
