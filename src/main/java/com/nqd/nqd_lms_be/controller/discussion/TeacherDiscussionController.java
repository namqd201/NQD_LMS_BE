package com.nqd.nqd_lms_be.controller.discussion;

import com.nqd.nqd_lms_be.common.dto.ApiResponse;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.discussion.DiscussionThreadResponse;
import com.nqd.nqd_lms_be.service.discussion.DiscussionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/courses/{courseId}/discussions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher - Discussion Moderation", description = "Teacher Moderation API for course discussion threads")
public class TeacherDiscussionController {

    private final DiscussionService discussionService;

    @PutMapping("/{threadId}/pin")
    @Operation(summary = "Pin or unpin discussion thread")
    public ResponseEntity<ApiResponse<DiscussionThreadResponse>> pinThread(
            @PathVariable UUID courseId,
            @PathVariable UUID threadId,
            @RequestParam boolean isPinned,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        DiscussionThreadResponse response = discussionService.pinThread(courseId, threadId, principal.getId(), isPinned);
        return ResponseEntity.ok(ApiResponse.ok(isPinned ? "Ghim chủ đề thành công" : "Bỏ ghim chủ đề thành công", response));
    }

    @PutMapping("/{threadId}/lock")
    @Operation(summary = "Lock or unlock discussion thread")
    public ResponseEntity<ApiResponse<DiscussionThreadResponse>> lockThread(
            @PathVariable UUID courseId,
            @PathVariable UUID threadId,
            @RequestParam boolean isLocked,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        DiscussionThreadResponse response = discussionService.lockThread(courseId, threadId, principal.getId(), isLocked);
        return ResponseEntity.ok(ApiResponse.ok(isLocked ? "Khóa chủ đề thành công" : "Mở khóa chủ đề thành công", response));
    }
}
