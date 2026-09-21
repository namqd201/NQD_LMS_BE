package com.nqd.nqd_lms_be.controller.teacher;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.teacher.ReorderItemsRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherResourceRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherResourceResponse;
import com.nqd.nqd_lms_be.service.teacher.TeacherLessonResourceService;
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
@RequestMapping("/api/v1/teacher/lessons/{lessonId}/resources")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher - Lesson Resources", description = "Endpoints for teachers to manage lesson attachments and resources")
public class TeacherLessonResourceController {

    private final TeacherLessonResourceService teacherLessonResourceService;

    @GetMapping
    @Operation(summary = "Get all resources attached to a lesson")
    public ResponseEntity<List<TeacherResourceResponse>> getLessonResources(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherLessonResourceService.getLessonResources(principal.getId(), lessonId));
    }

    @PostMapping
    @Operation(summary = "Add a new resource to a lesson (PDF, VIDEO, LINK, DOCUMENT, etc.)")
    public ResponseEntity<TeacherResourceResponse> addResource(
            @PathVariable UUID lessonId,
            @Valid @RequestBody TeacherResourceRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teacherLessonResourceService.addResource(principal.getId(), lessonId, request));
    }

    @PutMapping("/{resourceId}")
    @Operation(summary = "Update an existing lesson resource")
    public ResponseEntity<TeacherResourceResponse> updateResource(
            @PathVariable UUID lessonId,
            @PathVariable UUID resourceId,
            @Valid @RequestBody TeacherResourceRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherLessonResourceService.updateResource(principal.getId(), lessonId, resourceId, request));
    }

    @DeleteMapping("/{resourceId}")
    @Operation(summary = "Delete a lesson resource")
    public ResponseEntity<MessageResponse> deleteResource(
            @PathVariable UUID lessonId,
            @PathVariable UUID resourceId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherLessonResourceService.deleteResource(principal.getId(), lessonId, resourceId);
        return ResponseEntity.ok(MessageResponse.of("Resource deleted successfully"));
    }

    @PutMapping("/reorder")
    @Operation(summary = "Reorder lesson resources")
    public ResponseEntity<MessageResponse> reorderResources(
            @PathVariable UUID lessonId,
            @Valid @RequestBody ReorderItemsRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        teacherLessonResourceService.reorderResources(principal.getId(), lessonId, request);
        return ResponseEntity.ok(MessageResponse.of("Resources reordered successfully"));
    }
}
