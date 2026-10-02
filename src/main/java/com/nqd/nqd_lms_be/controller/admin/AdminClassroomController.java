package com.nqd.nqd_lms_be.controller.admin;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.classroom.AdminClassroomStatsResponse;
import com.nqd.nqd_lms_be.dto.classroom.ClassroomResponse;
import com.nqd.nqd_lms_be.dto.classroom.UpdateClassroomStatusRequest;
import com.nqd.nqd_lms_be.entity.enums.ClassroomStatus;
import com.nqd.nqd_lms_be.service.classroom.ClassroomService;
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
@RequestMapping("/api/v1/admin/classrooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Classrooms", description = "Endpoints for administrator classroom management and moderation")
public class AdminClassroomController {

    private final ClassroomService classroomService;

    @GetMapping
    @Operation(summary = "Get all classrooms with optional search and status filter")
    public ResponseEntity<List<ClassroomResponse>> getAllClassrooms(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ClassroomStatus status
    ) {
        return ResponseEntity.ok(classroomService.getAllClassroomsForAdmin(query, status));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get classroom management overview statistics for admin")
    public ResponseEntity<AdminClassroomStatsResponse> getClassroomStats() {
        return ResponseEntity.ok(classroomService.getClassroomStatsForAdmin());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get classroom details by ID for admin")
    public ResponseEntity<ClassroomResponse> getClassroomById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(classroomService.getClassroomById(id, principal != null ? principal.getId() : null));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update classroom status (ACTIVE / ARCHIVED)")
    public ResponseEntity<ClassroomResponse> updateClassroomStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClassroomStatusRequest request
    ) {
        return ResponseEntity.ok(classroomService.updateClassroomStatusForAdmin(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Force permanently delete a classroom by admin")
    public ResponseEntity<MessageResponse> forceDeleteClassroom(@PathVariable UUID id) {
        classroomService.forceDeleteClassroomForAdmin(id);
        return ResponseEntity.ok(MessageResponse.of("Đã xóa vĩnh viễn lớp học thành công."));
    }
}
