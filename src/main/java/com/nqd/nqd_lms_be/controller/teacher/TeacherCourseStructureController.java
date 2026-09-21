package com.nqd.nqd_lms_be.controller.teacher;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.teacher.*;
import com.nqd.nqd_lms_be.service.teacher.TeacherCourseStructureService;
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
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher - Course Structure", description = "Endpoints for teachers to manage course chapters and lessons")
public class TeacherCourseStructureController {

    private final TeacherCourseStructureService teacherCourseStructureService;

    // Course status
    @PutMapping("/courses/{courseId}/publish")
    @Operation(summary = "Publish course to make it visible to students")
    public ResponseEntity<TeacherCourseResponse> publishCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherCourseStructureService.publishCourse(courseId, principal.getId()));
    }

    @PutMapping("/courses/{courseId}/archive")
    @Operation(summary = "Archive course")
    public ResponseEntity<TeacherCourseResponse> archiveCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherCourseStructureService.archiveCourse(courseId, principal.getId()));
    }

    @GetMapping("/courses/{courseId}/structure")
    @Operation(summary = "Get full course syllabus structure (all chapters and lessons including drafts)")
    public ResponseEntity<TeacherCourseDetailResponse> getCourseStructure(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherCourseStructureService.getCourseStructure(courseId, principal.getId()));
    }

    // Chapters
    @PostMapping("/courses/{courseId}/chapters")
    @Operation(summary = "Create a new chapter in course")
    public ResponseEntity<TeacherChapterResponse> createChapter(
            @PathVariable UUID courseId,
            @Valid @RequestBody TeacherChapterRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        TeacherChapterResponse response = teacherCourseStructureService.createChapter(courseId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/chapters/{chapterId}")
    @Operation(summary = "Update chapter details")
    public ResponseEntity<TeacherChapterResponse> updateChapter(
            @PathVariable UUID chapterId,
            @Valid @RequestBody TeacherChapterRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherCourseStructureService.updateChapter(chapterId, request, principal.getId()));
    }

    @PutMapping("/courses/{courseId}/chapters/reorder")
    @Operation(summary = "Reorder chapters in course")
    public ResponseEntity<MessageResponse> reorderChapters(
            @PathVariable UUID courseId,
            @Valid @RequestBody ReorderItemsRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherCourseStructureService.reorderChapters(courseId, request, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Chapters reordered successfully"));
    }

    @DeleteMapping("/chapters/{chapterId}")
    @Operation(summary = "Delete chapter")
    public ResponseEntity<MessageResponse> deleteChapter(
            @PathVariable UUID chapterId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherCourseStructureService.deleteChapter(chapterId, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Chapter deleted successfully"));
    }

    // Lessons
    @PostMapping("/chapters/{chapterId}/lessons")
    @Operation(summary = "Create a new lesson in chapter")
    public ResponseEntity<TeacherLessonResponse> createLesson(
            @PathVariable UUID chapterId,
            @Valid @RequestBody TeacherLessonRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        TeacherLessonResponse response = teacherCourseStructureService.createLesson(chapterId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/lessons/{lessonId}")
    @Operation(summary = "Update lesson details")
    public ResponseEntity<TeacherLessonResponse> updateLesson(
            @PathVariable UUID lessonId,
            @Valid @RequestBody TeacherLessonRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherCourseStructureService.updateLesson(lessonId, request, principal.getId()));
    }

    @PutMapping("/chapters/{chapterId}/lessons/reorder")
    @Operation(summary = "Reorder lessons in chapter")
    public ResponseEntity<MessageResponse> reorderLessons(
            @PathVariable UUID chapterId,
            @Valid @RequestBody ReorderItemsRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherCourseStructureService.reorderLessons(chapterId, request, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Lessons reordered successfully"));
    }

    @PutMapping("/lessons/{lessonId}/publish")
    @Operation(summary = "Publish lesson to make it visible to students")
    public ResponseEntity<TeacherLessonResponse> publishLesson(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherCourseStructureService.publishLesson(lessonId, principal.getId()));
    }

    @PutMapping("/lessons/{lessonId}/archive")
    @Operation(summary = "Archive lesson")
    public ResponseEntity<TeacherLessonResponse> archiveLesson(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherCourseStructureService.archiveLesson(lessonId, principal.getId()));
    }

    @DeleteMapping("/lessons/{lessonId}")
    @Operation(summary = "Delete lesson")
    public ResponseEntity<MessageResponse> deleteLesson(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherCourseStructureService.deleteLesson(lessonId, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Lesson deleted successfully"));
    }
}
