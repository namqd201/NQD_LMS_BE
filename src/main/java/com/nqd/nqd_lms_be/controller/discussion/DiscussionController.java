package com.nqd.nqd_lms_be.controller.discussion;

import com.nqd.nqd_lms_be.common.dto.ApiResponse;
import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.discussion.*;
import com.nqd.nqd_lms_be.entity.enums.DiscussionThreadStatus;
import com.nqd.nqd_lms_be.service.discussion.DiscussionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/discussions")
@RequiredArgsConstructor
@Tag(name = "Discussion & Q&A", description = "Endpoints for course discussion threads and replies")
public class DiscussionController {

    private final DiscussionService discussionService;

    @GetMapping
    @Operation(summary = "Get discussion threads of a course")
    public ResponseEntity<ApiResponse<PageResponse<DiscussionThreadResponse>>> getCourseThreads(
            @PathVariable UUID courseId,
            @RequestParam(required = false) UUID lessonId,
            @RequestParam(required = false) DiscussionThreadStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<DiscussionThreadResponse> response = discussionService.getCourseThreads(
                courseId, lessonId, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách chủ đề thảo luận thành công", response));
    }

    @GetMapping("/{threadId}")
    @Operation(summary = "Get thread detail by ID")
    public ResponseEntity<ApiResponse<DiscussionThreadResponse>> getThreadDetail(
            @PathVariable UUID courseId,
            @PathVariable UUID threadId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        UUID userId = principal != null ? principal.getId() : null;
        DiscussionThreadResponse response = discussionService.getThreadDetail(courseId, threadId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết chủ đề thành công", response));
    }

    @PostMapping
    @Operation(summary = "Create a new discussion thread")
    public ResponseEntity<ApiResponse<DiscussionThreadResponse>> createThread(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateDiscussionThreadRequest request
    ) {
        DiscussionThreadResponse response = discussionService.createThread(courseId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo chủ đề thảo luận thành công", response));
    }

    @PutMapping("/{threadId}")
    @Operation(summary = "Update discussion thread")
    public ResponseEntity<ApiResponse<DiscussionThreadResponse>> updateThread(
            @PathVariable UUID courseId,
            @PathVariable UUID threadId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody UpdateDiscussionThreadRequest request
    ) {
        DiscussionThreadResponse response = discussionService.updateThread(courseId, threadId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật chủ đề thành công", response));
    }

    @DeleteMapping("/{threadId}")
    @Operation(summary = "Delete discussion thread")
    public ResponseEntity<ApiResponse<Void>> deleteThread(
            @PathVariable UUID courseId,
            @PathVariable UUID threadId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        discussionService.deleteThread(courseId, threadId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Xóa chủ đề thảo luận thành công", null));
    }

    @GetMapping("/{threadId}/posts")
    @Operation(summary = "Get posts / replies of a thread")
    public ResponseEntity<ApiResponse<PageResponse<DiscussionPostResponse>>> getThreadPosts(
            @PathVariable UUID courseId,
            @PathVariable UUID threadId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID userId = principal != null ? principal.getId() : null;
        PageResponse<DiscussionPostResponse> response = discussionService.getThreadPosts(courseId, threadId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách phản hồi thành công", response));
    }

    @PostMapping("/{threadId}/posts")
    @Operation(summary = "Reply to a discussion thread")
    public ResponseEntity<ApiResponse<DiscussionPostResponse>> createPost(
            @PathVariable UUID courseId,
            @PathVariable UUID threadId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateDiscussionPostRequest request
    ) {
        DiscussionPostResponse response = discussionService.createPost(courseId, threadId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Gửi phản hồi thành công", response));
    }

    @PutMapping("/{threadId}/posts/{postId}")
    @Operation(summary = "Update reply post")
    public ResponseEntity<ApiResponse<DiscussionPostResponse>> updatePost(
            @PathVariable UUID courseId,
            @PathVariable UUID threadId,
            @PathVariable UUID postId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody UpdateDiscussionPostRequest request
    ) {
        DiscussionPostResponse response = discussionService.updatePost(courseId, threadId, postId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật phản hồi thành công", response));
    }

    @DeleteMapping("/{threadId}/posts/{postId}")
    @Operation(summary = "Delete reply post")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable UUID courseId,
            @PathVariable UUID threadId,
            @PathVariable UUID postId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        discussionService.deletePost(courseId, threadId, postId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Xóa phản hồi thành công", null));
    }

    @PostMapping("/{threadId}/posts/{postId}/upvote")
    @Operation(summary = "Toggle upvote for a reply post")
    public ResponseEntity<ApiResponse<DiscussionPostResponse>> toggleUpvote(
            @PathVariable UUID courseId,
            @PathVariable UUID threadId,
            @PathVariable UUID postId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        DiscussionPostResponse response = discussionService.toggleUpvote(courseId, threadId, postId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Thao tác upvote thành công", response));
    }

    @PostMapping("/{threadId}/posts/{postId}/mark-answer")
    @Operation(summary = "Mark or unmark reply as accepted answer")
    public ResponseEntity<ApiResponse<DiscussionPostResponse>> markAnswer(
            @PathVariable UUID courseId,
            @PathVariable UUID threadId,
            @PathVariable UUID postId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        DiscussionPostResponse response = discussionService.markAnswer(courseId, threadId, postId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật đáp án đúng thành công", response));
    }
}
