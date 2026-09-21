package com.nqd.nqd_lms_be.controller.teacher;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.teacher.TeacherStudentLessonProgressResponse;
import com.nqd.nqd_lms_be.service.teacher.TeacherStudentProgressService;
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
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher - Student Progress", description = "Endpoints for teachers to inspect student learning progress across courses and lessons")
public class TeacherStudentProgressController {

    private final TeacherStudentProgressService teacherStudentProgressService;

    @GetMapping("/courses/{courseId}/student-progress")
    @Operation(summary = "Get progress of all students in a course managed by teacher")
    public ResponseEntity<List<TeacherStudentLessonProgressResponse>> getCourseStudentProgress(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherStudentProgressService.getCourseStudentProgress(principal.getId(), courseId));
    }

    @GetMapping("/lessons/{lessonId}/student-progress")
    @Operation(summary = "Get progress of all students in a specific lesson managed by teacher")
    public ResponseEntity<List<TeacherStudentLessonProgressResponse>> getLessonStudentProgress(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherStudentProgressService.getLessonStudentProgress(principal.getId(), lessonId));
    }

    @GetMapping("/students/{studentId}/progress")
    @Operation(summary = "Get detailed progress, enrolled courses, and exam performance of a specific student")
    public ResponseEntity<com.nqd.nqd_lms_be.dto.teacher.TeacherStudentDetailProgressResponse> getStudentProgressDetail(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherStudentProgressService.getStudentProgressDetail(principal.getId(), studentId));
    }

    @PostMapping("/courses/{courseId}/students/{studentId}/lessons/{lessonId}/unlock")
    @Operation(summary = "Teacher or Admin manually unlocks a specific lesson for a student")
    public ResponseEntity<Void> unlockLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID studentId,
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherStudentProgressService.unlockLesson(principal.getId(), courseId, studentId, lessonId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/courses/{courseId}/students/{studentId}/unlock-all")
    @Operation(summary = "Teacher or Admin manually unlocks all lessons in a course for a student")
    public ResponseEntity<Void> unlockAllLessons(
            @PathVariable UUID courseId,
            @PathVariable UUID studentId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherStudentProgressService.unlockAllLessons(principal.getId(), courseId, studentId);
        return ResponseEntity.ok().build();
    }
}
