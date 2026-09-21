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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscussionServiceImpl implements DiscussionService {

    private final DiscussionThreadRepository threadRepository;
    private final DiscussionPostRepository postRepository;
    private final PostReactionRepository reactionRepository;
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
        return PageResponse.fromPage(page, DiscussionThreadResponse::fromEntity);
    }

    @Override
    @Transactional
    public DiscussionThreadResponse getThreadDetail(UUID courseId, UUID threadId, UUID currentUserId) {
        DiscussionThread thread = getThreadOrThrow(courseId, threadId);
        threadRepository.incrementViewCount(threadId);
        return DiscussionThreadResponse.fromEntity(thread);
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

        return DiscussionThreadResponse.fromEntity(thread);
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
        return DiscussionThreadResponse.fromEntity(thread);
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
        return DiscussionThreadResponse.fromEntity(thread);
    }

    @Override
    @Transactional
    public DiscussionThreadResponse lockThread(UUID courseId, UUID threadId, UUID teacherId, boolean isLocked) {
        DiscussionThread thread = getThreadOrThrow(courseId, threadId);
        validateTeacherOrAdmin(thread.getCourse(), teacherId);

        thread.setIsLocked(isLocked);
        thread = threadRepository.save(thread);
        log.info("Thread {} in course {} lock status updated to {} by teacher {}", threadId, courseId, isLocked, teacherId);
        return DiscussionThreadResponse.fromEntity(thread);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DiscussionPostResponse> getThreadPosts(UUID courseId, UUID threadId, UUID currentUserId, Pageable pageable) {
        getThreadOrThrow(courseId, threadId);
        Page<DiscussionPost> postPage = postRepository.findByThreadId(threadId, pageable);

        return PageResponse.fromPage(postPage, post -> {
            boolean isUpvoted = currentUserId != null && reactionRepository.existsByPostIdAndUserIdAndType(post.getId(), currentUserId, PostReactionType.UPVOTE);
            return DiscussionPostResponse.fromEntity(post, currentUserId, isUpvoted);
        });
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

        // Notify thread author and course teacher
        try {
            String title = "Phản hồi mới trong chủ đề thảo luận";
            String body = author.getFullName() + " vừa phản hồi câu hỏi: \"" + thread.getTitle() + "\"";
            String linkUrl = (thread.getLesson() != null)
                    ? "/courses/" + courseId + "/lessons/" + thread.getLesson().getId() + "?threadId=" + thread.getId()
                    : "/courses/" + courseId + "?tab=discussion&threadId=" + thread.getId();

            // Notify thread author if different from post author
            if (!thread.getAuthor().getId().equals(authorId)) {
                kafkaNotificationProducer.sendNotification(thread.getAuthor().getId(), "DISCUSSION_REPLY", title, body, linkUrl);
            }

            // Notify course teacher if teacher is not the replier and not the thread author
            if (course.getCreator() != null && !course.getCreator().getId().equals(authorId) && !course.getCreator().getId().equals(thread.getAuthor().getId())) {
                kafkaNotificationProducer.sendNotification(course.getCreator().getId(), "DISCUSSION_REPLY", title, body, linkUrl);
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch discussion reply notification: {}", ex.getMessage());
        }

        // Notify mentioned users
        notifyMentionedUsers(course, thread, thread.getLesson(), author, request.getMentionedUserIds(), "trong một phản hồi: \"" + truncate(post.getContent(), 80) + "\"");

        return DiscussionPostResponse.fromEntity(post, authorId, false);
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
        boolean isUpvoted = reactionRepository.existsByPostIdAndUserIdAndType(postId, userId, PostReactionType.UPVOTE);
        return DiscussionPostResponse.fromEntity(post, userId, isUpvoted);
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
    public DiscussionPostResponse toggleUpvote(UUID courseId, UUID threadId, UUID postId, UUID userId) {
        DiscussionPost post = getPostOrThrow(courseId, threadId, postId);
        User user = getUserOrThrow(userId);

        Optional<PostReaction> reactionOpt = reactionRepository.findByPostIdAndUserIdAndType(postId, userId, PostReactionType.UPVOTE);
        boolean isNowUpvoted;

        if (reactionOpt.isPresent()) {
            reactionRepository.delete(reactionOpt.get());
            postRepository.decrementUpvoteCount(postId);
            post.setUpvoteCount(Math.max(0, post.getUpvoteCount() - 1));
            isNowUpvoted = false;
        } else {
            PostReaction reaction = PostReaction.builder()
                    .post(post)
                    .user(user)
                    .type(PostReactionType.UPVOTE)
                    .build();
            reactionRepository.save(reaction);
            postRepository.incrementUpvoteCount(postId);
            post.setUpvoteCount(post.getUpvoteCount() + 1);
            isNowUpvoted = true;
        }

        return DiscussionPostResponse.fromEntity(post, userId, isNowUpvoted);
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

        post = postRepository.save(post);
        threadRepository.save(thread);

        // Notify post author if marked as accepted answer
        if (newAnswerStatus && !post.getAuthor().getId().equals(userId)) {
            try {
                String title = "Câu trả lời của bạn đã được chấp nhận làm đáp án đúng!";
                String body = "Trong câu hỏi \"" + thread.getTitle() + "\"";
                String linkUrl = "/courses/" + courseId + "?tab=discussion&threadId=" + thread.getId();
                kafkaNotificationProducer.sendNotification(post.getAuthor().getId(), "DISCUSSION_ANSWER_ACCEPTED", title, body, linkUrl);
            } catch (Exception ex) {
                log.warn("Failed to dispatch accepted answer notification: {}", ex.getMessage());
            }
        }

        boolean isUpvoted = reactionRepository.existsByPostIdAndUserIdAndType(postId, userId, PostReactionType.UPVOTE);
        return DiscussionPostResponse.fromEntity(post, userId, isUpvoted);
    }

    private Course getCourseOrThrow(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
    }

    private DiscussionThread getThreadOrThrow(UUID courseId, UUID threadId) {
        DiscussionThread thread = threadRepository.findByIdWithDetails(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("DiscussionThread", threadId));
        if (!thread.getCourse().getId().equals(courseId)) {
            throw new IllegalArgumentException("Chủ đề thảo luận không thuộc khóa học này.");
        }
        return thread;
    }

    private DiscussionPost getPostOrThrow(UUID courseId, UUID threadId, UUID postId) {
        DiscussionThread thread = getThreadOrThrow(courseId, threadId);
        DiscussionPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("DiscussionPost", postId));
        if (!post.getThread().getId().equals(thread.getId())) {
            throw new IllegalArgumentException("Câu trả lời không thuộc chủ đề thảo luận này.");
        }
        return post;
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private void validateParticipation(Course course, UUID userId) {
        boolean isTeacher = course.getCreator() != null && course.getCreator().getId().equals(userId);
        boolean isAdmin = SecurityUtils.isAdmin();
        if (isTeacher || isAdmin) {
            return;
        }

        boolean isEnrolled = enrollmentRepository.findByCourseIdAndStudentId(course.getId(), userId)
                .map(e -> e.getStatus() == EnrollmentStatus.ENROLLED || e.getStatus() == EnrollmentStatus.COMPLETED)
                .orElse(false);

        if (!isEnrolled) {
            throw new ForbiddenOperationException("Chỉ học viên đã tham gia khóa học mới được phép gửi thảo luận.");
        }
    }

    private void validateTeacherOrAdmin(Course course, UUID userId) {
        boolean isTeacher = course.getCreator() != null && course.getCreator().getId().equals(userId);
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isTeacher && !isAdmin) {
            throw new ForbiddenOperationException("Chỉ giảng viên của khóa học hoặc Quản trị viên mới có quyền thực hiện thao tác này.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentionCandidateResponse> getMentionCandidates(UUID courseId) {
        Course course = getCourseOrThrow(courseId);
        java.util.Map<UUID, MentionCandidateResponse> candidatesMap = new java.util.LinkedHashMap<>();

        // 1. Course Creator / Primary Teacher
        if (course.getCreator() != null) {
            User creator = course.getCreator();
            candidatesMap.put(creator.getId(), MentionCandidateResponse.builder()
                    .id(creator.getId())
                    .fullName(creator.getFullName())
                    .email(creator.getEmail())
                    .avatarUrl(creator.getAvatarUrl())
                    .roleInCourse("TEACHER")
                    .build());
        }

        // 2. Co-teachers
        try {
            List<CourseTeacher> coTeachers = courseTeacherRepository.findByCourseId(courseId);
            if (coTeachers != null) {
                for (CourseTeacher ct : coTeachers) {
                    if (ct.getTeacher() != null && !candidatesMap.containsKey(ct.getTeacher().getId())) {
                        User t = ct.getTeacher();
                        candidatesMap.put(t.getId(), MentionCandidateResponse.builder()
                                .id(t.getId())
                                .fullName(t.getFullName())
                                .email(t.getEmail())
                                .avatarUrl(t.getAvatarUrl())
                                .roleInCourse("TEACHER")
                                .build());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Could not query co-teachers for course {}: {}", courseId, ex.getMessage());
        }

        // 3. Enrolled students
        try {
            List<CourseEnrollment> enrollments = enrollmentRepository.findByCourseIdWithStudent(courseId);
            if (enrollments != null) {
                for (CourseEnrollment ce : enrollments) {
                    if (ce.getStudent() != null && !candidatesMap.containsKey(ce.getStudent().getId())) {
                        User s = ce.getStudent();
                        candidatesMap.put(s.getId(), MentionCandidateResponse.builder()
                                .id(s.getId())
                                .fullName(s.getFullName())
                                .email(s.getEmail())
                                .avatarUrl(s.getAvatarUrl())
                                .roleInCourse("STUDENT")
                                .build());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Could not query enrollments for course {}: {}", courseId, ex.getMessage());
        }

        return new java.util.ArrayList<>(candidatesMap.values());
    }

    private void notifyMentionedUsers(Course course, DiscussionThread thread, Lesson lesson, User author, List<UUID> mentionedUserIds, String contextSnippet) {
        if (mentionedUserIds == null || mentionedUserIds.isEmpty()) {
            return;
        }

        String linkUrl = (lesson != null)
                ? "/courses/" + course.getId() + "/lessons/" + lesson.getId() + "?threadId=" + thread.getId()
                : "/courses/" + course.getId() + "?tab=discussion&threadId=" + thread.getId();

        String title = author.getFullName() + " đã nhắc đến bạn trong thảo luận";
        String body = author.getFullName() + " đã nhắc đến bạn " + contextSnippet;

        mentionedUserIds.stream()
                .filter(id -> id != null && !id.equals(author.getId()))
                .distinct()
                .forEach(recipientId -> {
                    try {
                        kafkaNotificationProducer.sendNotification(recipientId, "DISCUSSION_MENTION", title, body, linkUrl);
                        log.info("Sent mention notification to user {} for thread {}", recipientId, thread.getId());
                    } catch (Exception ex) {
                        log.warn("Failed to dispatch mention notification to user {}: {}", recipientId, ex.getMessage());
                    }
                });
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
