package com.nqd.nqd_lms_be.service.discussion;

import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.dto.discussion.*;
import com.nqd.nqd_lms_be.entity.enums.DiscussionThreadStatus;
import com.nqd.nqd_lms_be.entity.enums.PostReactionType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DiscussionService {

    PageResponse<DiscussionThreadResponse> getCourseThreads(
            UUID courseId, UUID lessonId, DiscussionThreadStatus status, String search, Pageable pageable);

    PageResponse<DiscussionThreadResponse> getCourseThreads(
            UUID courseId, UUID lessonId, DiscussionThreadStatus status, String search, UUID currentUserId, Pageable pageable);

    DiscussionThreadResponse getThreadDetail(UUID courseId, UUID threadId, UUID currentUserId);

    DiscussionThreadResponse createThread(UUID courseId, UUID authorId, CreateDiscussionThreadRequest request);

    DiscussionThreadResponse updateThread(UUID courseId, UUID threadId, UUID userId, UpdateDiscussionThreadRequest request);

    void deleteThread(UUID courseId, UUID threadId, UUID userId);

    DiscussionThreadResponse pinThread(UUID courseId, UUID threadId, UUID teacherId, boolean isPinned);

    DiscussionThreadResponse lockThread(UUID courseId, UUID threadId, UUID teacherId, boolean isLocked);

    PageResponse<DiscussionPostResponse> getThreadPosts(UUID courseId, UUID threadId, UUID currentUserId, Pageable pageable);

    DiscussionPostResponse createPost(UUID courseId, UUID threadId, UUID authorId, CreateDiscussionPostRequest request);

    DiscussionPostResponse updatePost(UUID courseId, UUID threadId, UUID postId, UUID userId, UpdateDiscussionPostRequest request);

    void deletePost(UUID courseId, UUID threadId, UUID postId, UUID userId);

    DiscussionPostResponse toggleUpvote(UUID courseId, UUID threadId, UUID postId, UUID userId);

    DiscussionThreadResponse reactToThread(UUID courseId, UUID threadId, UUID userId, PostReactionType type);

    DiscussionPostResponse reactToPost(UUID courseId, UUID threadId, UUID postId, UUID userId, PostReactionType type);

    DiscussionPostResponse markAnswer(UUID courseId, UUID threadId, UUID postId, UUID userId);

    java.util.List<MentionCandidateResponse> getMentionCandidates(UUID courseId);
}
