package com.nqd.nqd_lms_be.service.discussion;

import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.discussion.DiscussionRateLimiter;
import com.nqd.nqd_lms_be.dto.discussion.*;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.DiscussionThreadStatus;
import com.nqd.nqd_lms_be.entity.enums.EnrollmentStatus;
import com.nqd.nqd_lms_be.entity.enums.PostReactionType;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscussionServiceImpl implements DiscussionService {

    private final DiscussionThreadRepository threadRepository;
    private final DiscussionPostRepository postRepository;
    private final PostReactionRepository reactionRepository;
    private final ThreadReactionRepository threadReactionRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final UserRepository userRepository;
    private final DiscussionRateLimiter rateLimiter;
    private final KafkaNotificationProducer kafkaNotificationProducer;
    private final com.nqd.nqd_lms_be.service.moderation.ContentModerationService contentModerationService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DiscussionThreadResponse> getCourseThreads(
            UUID courseId, UUID lessonId, DiscussionThreadStatus status, String search, Pageable pageable) {
        return getCourseThreads(courseId, lessonId, status, search, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DiscussionThreadResponse> getCourseThreads(
            UUID courseId, UUID lessonId, DiscussionThreadStatus status, String search, UUID currentUserId, Pageable pageable) {

        Course course = getCourseOrThrow(courseId);

        Specification<DiscussionThread> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("course").get("id"), courseId));
            predicates.add(cb.or(cb.isNull(root.get("isDeleted")), cb.isFalse(root.get("isDeleted"))));

            if (lessonId != null) {
                predicates.add(cb.equal(root.get("lesson").get("id"), lessonId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate contentLike = cb.like(cb.lower(root.get("content")), pattern);
                predicates.add(cb.or(titleLike, contentLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<DiscussionThread> page = threadRepository.findAll(spec, pageable);
        return PageResponse.fromPage(page, thread -> buildThreadResponse(thread, currentUserId));
    }

    @Override
    @Transactional
    public DiscussionThreadResponse getThreadDetail(UUID courseId, UUID threadId, UUID currentUserId) {
        DiscussionThread thread = getThreadOrThrow(courseId, threadId);
        threadRepository.incrementViewCount(threadId);
        return buildThreadResponse(thread, currentUserId);
    }

    @Override
    @Transactional
    public DiscussionThreadResponse createThread(UUID courseId, UUID authorId, CreateDiscussionThreadRequest request) {
        rateLimiter.checkAndAcquire(authorId);

        Course course = getCourseOrThrow(courseId);
        User author = getUserOrThrow(authorId);

        validateParticipation(course, authorId);

        contentModerationService.validateContent(request.getTitle(), "Tiêu đề thảo luận");
        contentModerationService.validateContent(request.getContent(), "Nội dung thảo luận");

        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", request.getLessonId()));
        }

        DiscussionThread thread = DiscussionThread.builder()
                .course(course)
                .lesson(lesson)
                .author(author)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .isPinned(false)
                .isLocked(false)
                .status(DiscussionThreadStatus.OPEN)
                .postCount(0)
                .viewCount(0)
                .build();

        thread = threadRepository.save(thread);
        log.info("User {} created discussion thread {} in course {}", author.getEmail(), thread.getId(), courseId);

        // Notify course creator if author is not the teacher
        if (course.getCreator() != null && !course.getCreator().getId().equals(authorId)) {
            try {
                String title = "Câu hỏi mới trong khóa học: " + course.getName();
                String body = author.getFullName() + " vừa đặt câu hỏi: \"" + thread.getTitle() + "\"";
                String linkUrl = "/courses/" + courseId + "?tab=discussion&threadId=" + thread.getId();
                kafkaNotificationProducer.sendNotification(course.getCreator().getId(), "DISCUSSION_NEW_THREAD", title, body, linkUrl);
            } catch (Exception ex) {
                log.warn("Failed to dispatch thread creation notification: {}", ex.getMessage());
            }
        }

        // Notify mentioned users
        notifyMentionedUsers(course, thread, lesson, author, request.getMentionedUserIds(), "trong một chủ đề thảo luận: \"" + thread.getTitle() + "\"");

        return buildThreadResponse(thread, authorId);
    }

    @Override
    @Transactional
    public DiscussionThreadResponse updateThread(UUID courseId, UUID threadId, UUID userId, UpdateDiscussionThreadRequest request) {
        DiscussionThread thread = getThreadOrThrow(courseId, threadId);

        if (!thread.getAuthor().getId().equals(userId) && !SecurityUtils.isAdmin()) {
            throw new ForbiddenOperationException("Bạn không có quyền chỉnh sửa chủ đề thảo luận này.");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            contentModerationService.validateContent(request.getTitle(), "Tiêu đề thảo luận");
            thread.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null && !request.getContent().isBlank()) {
            contentModerationService.validateContent(request.getContent(), "Nội dung thảo luận");
            thread.setContent(request.getContent().trim());
        }

        thread = threadRepository.save(thread);
        return buildThreadResponse(thread, userId);
    }

    @Override
    @Transactional
    public void deleteThread(UUID courseId, UUID threadId, UUID userId) {
        DiscussionThread thread = getThreadOrThrow(courseId, threadId);
        Course course = thread.getCourse();

        boolean isAuthor = thread.getAuthor().getId().equals(userId);
        boolean isTeacher = course.getCreator() != null && course.getCreator().getId().equals(userId);
        boolean isAdmin = SecurityUtils.isAdmin();

        if (!isAuthor && !isTeacher && !isAdmin) {
            throw new ForbiddenOperationException("Bạn không có quyền xóa chủ đề thảo luận này.");
        }

        thread.setIsDeleted(true);
        threadRepository.save(thread);
        log.info("Thread {} in course {} deleted by user {}", threadId, courseId, userId);
    }

    @Override
    @Transactional
    public DiscussionThreadResponse pinThread(UUID courseId, UUID threadId, UUID teacherId, boolean isPinned) {
        DiscussionThread thread = getThreadOrThrow(courseId, threadId);
        validateTeacherOrAdmin(thread.getCourse(), teacherId);

        thread.setIsPinned(isPinned);
        thread = threadRepository.save(thread);
        log.info("Thread {} in course {} pin status updated to {} by teacher {}", threadId, courseId, isPinned, teacherId);
        return buildThreadResponse(thread, teacherId);
    }

    @Override
    @Transactional
    public DiscussionThreadResponse lockThread(UUID courseId, UUID threadId, UUID teacherId, boolean isLocked) {
        DiscussionThread thread = getThreadOrThrow(courseId, threadId);
        validateTeacherOrAdmin(thread.getCourse(), teacherId);

        thread.setIsLocked(isLocked);
        thread = threadRepository.save(thread);
        log.info("Thread {} in course {} lock status updated to {} by teacher {}", threadId, courseId, isLocked, teacherId);
        return buildThreadResponse(thread, teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DiscussionPostResponse> getThreadPosts(UUID courseId, UUID threadId, UUID currentUserId, Pageable pageable) {
        getThreadOrThrow(courseId, threadId);
        Page<DiscussionPost> postPage = postRepository.findByThreadId(threadId, pageable);

        return PageResponse.fromPage(postPage, post -> buildPostResponse(post, currentUserId));
    }

    @Override
    @Transactional
    public DiscussionPostResponse createPost(UUID courseId, UUID threadId, UUID authorId, CreateDiscussionPostRequest request) {
        rateLimiter.checkAndAcquire(authorId);

        DiscussionThread thread = getThreadOrThrow(courseId, threadId);
        if (Boolean.TRUE.equals(thread.getIsLocked())) {
            throw new ForbiddenOperationException("Chủ đề thảo luận này đã bị khóa, không thể gửi thêm phản hồi.");
        }

        Course course = thread.getCourse();
        User author = getUserOrThrow(authorId);

        validateParticipation(course, authorId);

        contentModerationService.validateContent(request.getContent(), "Nội dung phản hồi");

        DiscussionPost parentPost = null;
        if (request.getParentId() != null) {
            parentPost = postRepository.findById(request.getParentId()).orElse(null);
        }

        DiscussionPost post = DiscussionPost.builder()
                .thread(thread)
                .author(author)
                .parent(parentPost)
                .content(request.getContent().trim())
                .isAnswer(false)
                .upvoteCount(0)
                .build();

        post = postRepository.save(post);
        threadRepository.incrementPostCount(threadId);

        log.info("User {} replied to thread {} in course {}", author.getEmail(), threadId, courseId);

        // Notify thread author if replier is not thread author
        try {
            if (!thread.getAuthor().getId().equals(authorId)) {
                String title = "Phản hồi mới trong thảo luận: " + thread.getTitle();
                String body = author.getFullName() + " vừa trả lời trong chủ đề thảo luận của bạn.";
                String linkUrl = "/courses/" + courseId + "?tab=discussion&threadId=" + threadId;
                kafkaNotificationProducer.sendNotification(thread.getAuthor().getId(), "DISCUSSION_NEW_REPLY", title, body, linkUrl);
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch discussion reply notification: {}", ex.getMessage());
        }

        // Notify mentioned users
        notifyMentionedUsers(course, thread, thread.getLesson(), author, request.getMentionedUserIds(), "trong một phản hồi: \"" + truncate(post.getContent(), 80) + "\"");

        return buildPostResponse(post, authorId);
    }

    @Override
    @Transactional
    public DiscussionPostResponse updatePost(UUID courseId, UUID threadId, UUID postId, UUID userId, UpdateDiscussionPostRequest request) {
        DiscussionPost post = getPostOrThrow(courseId, threadId, postId);

        if (!post.getAuthor().getId().equals(userId) && !SecurityUtils.isAdmin()) {
            throw new ForbiddenOperationException("Bạn không có quyền chỉnh sửa câu trả lời này.");
        }

        contentModerationService.validateContent(request.getContent(), "Nội dung phản hồi");

        post.setContent(request.getContent().trim());
        post = postRepository.save(post);
        return buildPostResponse(post, userId);
    }

    @Override
    @Transactional
    public void deletePost(UUID courseId, UUID threadId, UUID postId, UUID userId) {
        DiscussionPost post = getPostOrThrow(courseId, threadId, postId);
        Course course = post.getThread().getCourse();

        boolean isAuthor = post.getAuthor().getId().equals(userId);
        boolean isTeacher = course.getCreator() != null && course.getCreator().getId().equals(userId);
        boolean isAdmin = SecurityUtils.isAdmin();

        if (!isAuthor && !isTeacher && !isAdmin) {
            throw new ForbiddenOperationException("Bạn không có quyền xóa câu trả lời này.");
        }

        post.setIsDeleted(true);
        postRepository.save(post);
        threadRepository.decrementPostCount(threadId);
        log.info("Post {} in thread {} deleted by user {}", postId, threadId, userId);
    }

    @Override
    @Transactional
    public DiscussionThreadResponse reactToThread(UUID courseId, UUID threadId, UUID userId, PostReactionType type) {
        DiscussionThread thread = getThreadOrThrow(courseId, threadId);
        User user = getUserOrThrow(userId);
        if (type == null || type == PostReactionType.UPVOTE) {
            type = PostReactionType.LIKE;
        }

        Optional<ThreadReaction> reactionOpt = threadReactionRepository.findByThreadIdAndUserId(threadId, userId);
        if (reactionOpt.isPresent()) {
            ThreadReaction existing = reactionOpt.get();
            if (existing.getType() == type) {
                threadReactionRepository.delete(existing);
            } else {
                existing.setType(type);
                threadReactionRepository.save(existing);
            }
        } else {
            ThreadReaction reaction = ThreadReaction.builder()
                    .thread(thread)
                    .user(user)
                    .type(type)
                    .build();
            threadReactionRepository.save(reaction);
        }

        return buildThreadResponse(thread, userId);
    }

    @Override
    @Transactional
    public DiscussionPostResponse reactToPost(UUID courseId, UUID threadId, UUID postId, UUID userId, PostReactionType type) {
        DiscussionPost post = getPostOrThrow(courseId, threadId, postId);
        User user = getUserOrThrow(userId);
        if (type == null || type == PostReactionType.UPVOTE) {
            type = PostReactionType.LIKE;
        }

        Optional<PostReaction> reactionOpt = reactionRepository.findByPostIdAndUserId(postId, userId);
        if (reactionOpt.isPresent()) {
            PostReaction existing = reactionOpt.get();
            if (existing.getType() == type) {
                reactionRepository.delete(existing);
                postRepository.decrementUpvoteCount(postId);
                post.setUpvoteCount(Math.max(0, (post.getUpvoteCount() != null ? post.getUpvoteCount() : 1) - 1));
            } else {
                existing.setType(type);
                reactionRepository.save(existing);
            }
        } else {
            PostReaction reaction = PostReaction.builder()
                    .post(post)
                    .user(user)
                    .type(type)
                    .build();
            reactionRepository.save(reaction);
            postRepository.incrementUpvoteCount(postId);
            post.setUpvoteCount((post.getUpvoteCount() != null ? post.getUpvoteCount() : 0) + 1);
        }

        return buildPostResponse(post, userId);
    }

    @Override
    @Transactional
    public DiscussionPostResponse toggleUpvote(UUID courseId, UUID threadId, UUID postId, UUID userId) {
        return reactToPost(courseId, threadId, postId, userId, PostReactionType.LIKE);
    }

    @Override
    @Transactional
    public DiscussionPostResponse markAnswer(UUID courseId, UUID threadId, UUID postId, UUID userId) {
        DiscussionPost post = getPostOrThrow(courseId, threadId, postId);
        DiscussionThread thread = post.getThread();
        Course course = thread.getCourse();

        boolean isThreadAuthor = thread.getAuthor().getId().equals(userId);
        boolean isTeacher = course.getCreator() != null && course.getCreator().getId().equals(userId);
        boolean isAdmin = SecurityUtils.isAdmin();

        if (!isThreadAuthor && !isTeacher && !isAdmin) {
            throw new ForbiddenOperationException("Chỉ chủ câu hỏi hoặc giảng viên phụ trách mới có quyền đánh dấu đáp án đúng.");
        }

        boolean newAnswerStatus = !Boolean.TRUE.equals(post.getIsAnswer());
        if (newAnswerStatus) {
            postRepository.resetOtherAnswersInThread(threadId, postId);
            post.setIsAnswer(true);
            thread.setStatus(DiscussionThreadStatus.RESOLVED);
        } else {
            post.setIsAnswer(false);
            thread.setStatus(DiscussionThreadStatus.OPEN);
        }

        postRepository.save(post);
        threadRepository.save(thread);
        log.info("Post {} marked as answer={} by user {}", postId, newAnswerStatus, userId);

        return buildPostResponse(post, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentionCandidateResponse> getMentionCandidates(UUID courseId) {
        Course course = getCourseOrThrow(courseId);

        Map<UUID, MentionCandidateResponse> candidates = new LinkedHashMap<>();

        // 1. Course Creator / Main Teacher
        if (course.getCreator() != null) {
            User c = course.getCreator();
            candidates.put(c.getId(), MentionCandidateResponse.builder()
                    .id(c.getId())
                    .fullName(c.getFullName())
                    .email(c.getEmail())
                    .avatarUrl(c.getAvatarUrl())
                    .roleInCourse("TEACHER")
                    .build());
        }

        // 2. Co-teachers
        List<CourseTeacher> coTeachers = courseTeacherRepository.findByCourseId(courseId);
        for (CourseTeacher ct : coTeachers) {
            User t = ct.getTeacher();
            if (t != null && !candidates.containsKey(t.getId())) {
                candidates.put(t.getId(), MentionCandidateResponse.builder()
                        .id(t.getId())
                        .fullName(t.getFullName())
                        .email(t.getEmail())
                        .avatarUrl(t.getAvatarUrl())
                        .roleInCourse("TEACHER")
                        .build());
            }
        }

        // 3. Enrolled Students
        List<CourseEnrollment> enrollments = enrollmentRepository.findByCourseIdAndStatus(courseId, EnrollmentStatus.ENROLLED);
        for (CourseEnrollment e : enrollments) {
            User s = e.getStudent();
            if (s != null && !candidates.containsKey(s.getId())) {
                candidates.put(s.getId(), MentionCandidateResponse.builder()
                        .id(s.getId())
                        .fullName(s.getFullName())
                        .email(s.getEmail())
                        .avatarUrl(s.getAvatarUrl())
                        .roleInCourse("STUDENT")
                        .build());
            }
        }

        return new ArrayList<>(candidates.values());
    }

    private DiscussionThreadResponse buildThreadResponse(DiscussionThread thread, UUID currentUserId) {
        List<ThreadReaction> reactions = threadReactionRepository.findByThreadId(thread.getId());
        int count = reactions.size();
        PostReactionType myReaction = currentUserId != null
                ? reactions.stream()
                .filter(r -> r.getUser() != null && r.getUser().getId().equals(currentUserId))
                .map(ThreadReaction::getType)
                .findFirst()
                .orElse(null)
                : null;

        Map<String, Integer> breakdown = reactions.stream()
                .collect(Collectors.groupingBy(r -> (r.getType() == PostReactionType.UPVOTE ? PostReactionType.LIKE : r.getType()).name(), Collectors.summingInt(r -> 1)));

        return DiscussionThreadResponse.fromEntity(thread, currentUserId, myReaction, breakdown, count);
    }

    private DiscussionPostResponse buildPostResponse(DiscussionPost post, UUID currentUserId) {
        List<PostReaction> reactions = reactionRepository.findByPostId(post.getId());
        int count = reactions.size();
        PostReactionType myReaction = currentUserId != null
                ? reactions.stream()
                .filter(r -> r.getUser() != null && r.getUser().getId().equals(currentUserId))
                .map(r -> r.getType() == PostReactionType.UPVOTE ? PostReactionType.LIKE : r.getType())
                .findFirst()
                .orElse(null)
                : null;

        Map<String, Integer> breakdown = reactions.stream()
                .collect(Collectors.groupingBy(r -> (r.getType() == PostReactionType.UPVOTE ? PostReactionType.LIKE : r.getType()).name(), Collectors.summingInt(r -> 1)));

        return DiscussionPostResponse.fromEntity(post, currentUserId, myReaction, breakdown, count);
    }

    private void notifyMentionedUsers(Course course, DiscussionThread thread, Lesson lesson, User author, List<UUID> mentionedUserIds, String contextDesc) {
        if (mentionedUserIds == null || mentionedUserIds.isEmpty()) return;

        for (UUID mentionedUserId : mentionedUserIds) {
            if (mentionedUserId.equals(author.getId())) continue;

            try {
                String title = author.getFullName() + " đã nhắc đến bạn";
                String body = author.getFullName() + " đã nhắc đến bạn " + contextDesc;
                String linkUrl = "/courses/" + course.getId() + "?tab=discussion&threadId=" + thread.getId();
                kafkaNotificationProducer.sendNotification(mentionedUserId, "DISCUSSION_MENTION", title, body, linkUrl);
            } catch (Exception ex) {
                log.warn("Failed to dispatch mention notification to user {}: {}", mentionedUserId, ex.getMessage());
            }
        }
    }

    private Course getCourseOrThrow(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
    }

    private DiscussionThread getThreadOrThrow(UUID courseId, UUID threadId) {
        DiscussionThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("DiscussionThread", threadId));
        if (!thread.getCourse().getId().equals(courseId)) {
            throw new ResourceNotFoundException("DiscussionThread in Course", threadId);
        }
        if (Boolean.TRUE.equals(thread.getIsDeleted())) {
            throw new ResourceNotFoundException("DiscussionThread", threadId);
        }
        return thread;
    }

    private DiscussionPost getPostOrThrow(UUID courseId, UUID threadId, UUID postId) {
        DiscussionThread thread = getThreadOrThrow(courseId, threadId);
        DiscussionPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("DiscussionPost", postId));
        if (!post.getThread().getId().equals(thread.getId())) {
            throw new ResourceNotFoundException("DiscussionPost in Thread", postId);
        }
        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new ResourceNotFoundException("DiscussionPost", postId);
        }
        return post;
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private void validateParticipation(Course course, UUID userId) {
        if (SecurityUtils.isAdmin()) return;
        if (course.getCreator() != null && course.getCreator().getId().equals(userId)) return;
        if (courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), userId)) return;
        if (enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(course.getId(), userId, EnrollmentStatus.ENROLLED)) return;

        throw new ForbiddenOperationException("Bạn cần đăng ký khóa học để tham gia thảo luận.");
    }

    private void validateTeacherOrAdmin(Course course, UUID userId) {
        if (SecurityUtils.isAdmin()) return;
        if (course.getCreator() != null && course.getCreator().getId().equals(userId)) return;
        if (courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), userId)) return;

        throw new ForbiddenOperationException("Chỉ giảng viên của khóa học mới có quyền thực hiện thao tác này.");
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen - 3) + "..." : text;
    }
}
