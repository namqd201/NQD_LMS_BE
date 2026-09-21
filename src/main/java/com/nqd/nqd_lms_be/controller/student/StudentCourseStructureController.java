package com.nqd.nqd_lms_be.controller.student;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.student.StudentCourseDetailResponse;
import com.nqd.nqd_lms_be.dto.student.StudentLessonDetailResponse;
import com.nqd.nqd_lms_be.service.student.StudentCourseStructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
@Tag(name = "Student - Course Structure", description = "Endpoints for students to browse published syllabus and lessons")
public class StudentCourseStructureController {

    private final StudentCourseStructureService studentCourseStructureService;

    @GetMapping("/courses/{courseId}/structure")
    @Operation(summary = "Get published course syllabus (Only published chapters and lessons)")
    public ResponseEntity<StudentCourseDetailResponse> getPublishedCourseStructure(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentCourseStructureService.getPublishedCourseDetail(courseId, principal.getId()));
    }

    @GetMapping("/lessons/{lessonId}")
    @Operation(summary = "Get published lesson content")
    public ResponseEntity<StudentLessonDetailResponse> getPublishedLesson(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentCourseStructureService.getPublishedLesson(lessonId, principal.getId()));
    }
}
