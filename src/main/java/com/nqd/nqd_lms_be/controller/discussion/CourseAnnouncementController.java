package com.nqd.nqd_lms_be.controller.discussion;

import com.nqd.nqd_lms_be.common.dto.ApiResponse;
import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.discussion.CourseAnnouncementResponse;
import com.nqd.nqd_lms_be.dto.discussion.CreateCourseAnnouncementRequest;
import com.nqd.nqd_lms_be.service.discussion.CourseAnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/announcements")
@RequiredArgsConstructor
@Tag(name = "Course Announcements", description = "Endpoints for course announcements")
public class CourseAnnouncementController {

    private final CourseAnnouncementService announcementService;

    @GetMapping
    @Operation(summary = "Get announcements for a course")
    public ResponseEntity<ApiResponse<PageResponse<CourseAnnouncementResponse>>> getAnnouncements(
            @PathVariable UUID courseId,
            @PageableDefault(size = 10, sort = "postedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<CourseAnnouncementResponse> response = announcementService.getCourseAnnouncements(courseId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách thông báo thành công", response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Create an announcement for a course (Teacher/Admin only)")
    public ResponseEntity<ApiResponse<CourseAnnouncementResponse>> createAnnouncement(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateCourseAnnouncementRequest request
    ) {
        CourseAnnouncementResponse response = announcementService.createAnnouncement(courseId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đăng thông báo khóa học thành công", response));
    }

    @DeleteMapping("/{announcementId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Delete an announcement (Teacher/Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @PathVariable UUID courseId,
            @PathVariable UUID announcementId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        announcementService.deleteAnnouncement(courseId, announcementId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Xóa thông báo thành công", null));
    }
}
