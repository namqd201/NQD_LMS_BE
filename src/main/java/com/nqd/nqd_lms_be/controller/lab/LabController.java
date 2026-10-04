package com.nqd.nqd_lms_be.controller.lab;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.lab.CreateLabRoomRequest;
import com.nqd.nqd_lms_be.dto.lab.CreateLabVideoRequest;
import com.nqd.nqd_lms_be.dto.lab.LabRecordedVideoResponse;
import com.nqd.nqd_lms_be.dto.lab.LabRoomResponse;
import com.nqd.nqd_lms_be.entity.enums.LabStatus;
import com.nqd.nqd_lms_be.service.lab.LabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Open Lab - Training & Video Call", description = "Public knowledge training labs and private recorded videos")
public class LabController {

    private final LabService labService;

    // ==========================================
    // PUBLIC LAB ROOM ENDPOINTS
    // ==========================================

    @GetMapping("/api/v1/public/labs")
    @Operation(summary = "Get list of upcoming and live public lab rooms")
    public ResponseEntity<List<LabRoomResponse>> getPublicLabRooms(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        boolean isAdmin = principal != null && principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
        return ResponseEntity.ok(labService.getPublicLabRooms(currentUserId, isAdmin));
    }

    @GetMapping("/api/v1/public/labs/{id}")
    @Operation(summary = "Get public lab room details")
    public ResponseEntity<LabRoomResponse> getPublicLabRoom(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        boolean isAdmin = principal != null && principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
        return ResponseEntity.ok(labService.getLabRoomById(id, currentUserId, isAdmin));
    }

    // ==========================================
    // AUTHENTICATED LAB ROOM MANAGEMENT
    // ==========================================

    @PostMapping("/api/v1/labs")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Register/Create a new open Lab room (For teachers, lecturers, doctors, professors...)")
    public ResponseEntity<LabRoomResponse> createLabRoom(
            @Valid @RequestBody CreateLabRoomRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(labService.createLabRoom(principal.getId(), request));
    }

    @PutMapping("/api/v1/labs/{id}/status")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Update lab room status (e.g. LIVE or ENDED)")
    public ResponseEntity<LabRoomResponse> updateLabStatus(
            @PathVariable UUID id,
            @RequestParam LabStatus status,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
        return ResponseEntity.ok(labService.updateLabStatus(id, principal.getId(), isAdmin, status));
    }

    @DeleteMapping("/api/v1/labs/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Delete/Close a lab room")
    public ResponseEntity<Void> deleteLabRoom(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
        labService.deleteLabRoom(id, principal.getId(), isAdmin);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // PRIVATE RECORDED VIDEOS (HOST & ADMIN ONLY)
    // ==========================================

    @GetMapping("/api/v1/labs/videos/my")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
    @Operation(summary = "Get private recorded videos (Only for Host and Admin)")
    public ResponseEntity<List<LabRecordedVideoResponse>> getRecordedVideos(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
        return ResponseEntity.ok(labService.getRecordedVideos(principal.getId(), isAdmin));
    }

    @PostMapping("/api/v1/labs/{id}/videos")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Save a recorded video for a lab room")
    public ResponseEntity<LabRecordedVideoResponse> saveRecordedVideo(
            @PathVariable UUID id,
            @Valid @RequestBody CreateLabVideoRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
        return ResponseEntity.ok(labService.saveRecordedVideo(id, principal.getId(), isAdmin, request));
    }

    @PostMapping("/api/v1/labs/{id}/sync-recordings")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Sync recordings from 100ms for a lab room")
    public ResponseEntity<Map<String, Object>> sync100msRecordings(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
        int synced = labService.sync100msRecordings(id, principal.getId(), isAdmin);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "syncedCount", synced,
                "message", "Đã đồng bộ thành công " + synced + " video bản ghi mới từ 100ms."
        ));
    }

    @DeleteMapping("/api/v1/labs/videos/{videoId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Delete a recorded video")
    public ResponseEntity<Void> deleteRecordedVideo(
            @PathVariable UUID videoId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
        labService.deleteRecordedVideo(videoId, principal.getId(), isAdmin);
        return ResponseEntity.noContent().build();
    }
}
