package com.nqd.nqd_lms_be.discussion;

import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.dto.discussion.*;
import com.nqd.nqd_lms_be.dto.teacher.TeacherChapterRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherCourseRequest;
import com.nqd.nqd_lms_be.dto.teacher.TeacherCourseResponse;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.service.course.CourseWorkflowService;
import com.nqd.nqd_lms_be.service.discussion.CourseAnnouncementService;
import com.nqd.nqd_lms_be.service.discussion.DiscussionService;
import com.nqd.nqd_lms_be.service.teacher.TeacherCourseService;
import com.nqd.nqd_lms_be.service.teacher.TeacherCourseStructureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DiscussionIntegrationTest {

    @Autowired
    private DiscussionService discussionService;

    @Autowired
    private CourseAnnouncementService announcementService;

    @Autowired
    private TeacherCourseService teacherCourseService;

    @Autowired
    private TeacherCourseStructureService teacherCourseStructureService;

    @Autowired
    private CourseWorkflowService courseWorkflowService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CourseEnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private DiscussionThreadRepository threadRepository;

    @Autowired
    private DiscussionPostRepository postRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private User teacherUser;
    private User studentUser1;
    private User studentUser2;
    private User adminUser;
    private Subject mathSubject;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        Role teacherRole = roleRepository.findByName("ROLE_TEACHER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_TEACHER").build()));
        Role studentRole = roleRepository.findByName("ROLE_STUDENT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_STUDENT").build()));
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));

        teacherUser = userRepository.save(User.builder()
                .email("teacher_disc_" + UUID.randomUUID() + "@test.com")
                .fullName("Thầy Giáo Thảo Luận")
                .status(UserStatus.ACTIVE)
                .build());

        studentUser1 = userRepository.save(User.builder()
                .email("student1_disc_" + UUID.randomUUID() + "@test.com")
                .fullName("Học Viên 1")
                .status(UserStatus.ACTIVE)
                .build());

        studentUser2 = userRepository.save(User.builder()
                .email("student2_disc_" + UUID.randomUUID() + "@test.com")
                .fullName("Học Viên 2")
                .status(UserStatus.ACTIVE)
                .build());

        adminUser = userRepository.save(User.builder()
                .email("admin_disc_" + UUID.randomUUID() + "@test.com")
                .fullName("Quản Trị Viên")
                .status(UserStatus.ACTIVE)
                .build());

        mathSubject = subjectRepository.save(Subject.builder()
                .name("Toán Học " + UUID.randomUUID().toString().substring(0, 5))
                .code("MATH_" + UUID.randomUUID().toString().substring(0, 5).toUpperCase())
                .build());

        authenticateAs(teacherUser, "TEACHER");
        TeacherCourseResponse courseDto = teacherCourseService.createCourse(
                TeacherCourseRequest.builder()
                        .subjectId(mathSubject.getId())
                        .name("Khóa Học Thảo Luận & Q&A")
                        .code("DISC-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase())
                        .pricingType(CoursePricingType.FREE)
                        .price(BigDecimal.ZERO)
                        .build(),
                teacherUser.getId()
        );

        teacherCourseStructureService.createChapter(courseDto.getId(), TeacherChapterRequest.builder().title("Chương 1").build(), teacherUser.getId());
        courseWorkflowService.submitForReview(courseDto.getId(), teacherUser.getId());

        authenticateAs(adminUser, "ADMIN");
        courseWorkflowService.approveCourse(courseDto.getId(), adminUser.getId());

        testCourse = courseRepository.findById(courseDto.getId()).orElseThrow();
    }

    private void authenticateAs(User user, String role) {
        AppUserPrincipal principal = new AppUserPrincipal(
                user,
                Set.of(role),
                Map.of("sub", user.getId().toString(), "email", user.getEmail()),
                null,
                null
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("1. Unenrolled student is blocked from creating a discussion thread")
    void testCreateThread_UnenrolledForbidden() {
        authenticateAs(studentUser1, "STUDENT");

        CreateDiscussionThreadRequest request = CreateDiscussionThreadRequest.builder()
                .title("Cho em hỏi bài 1?")
                .content("Em chưa hiểu phương pháp giải câu này.")
                .build();

        assertThatThrownBy(() -> discussionService.createThread(testCourse.getId(), studentUser1.getId(), request))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Chỉ học viên đã tham gia");
    }

    @Test
    @DisplayName("2. Enrolled student creates discussion thread successfully")
    void testCreateThread_Success() {
        enrollmentRepository.save(CourseEnrollment.builder()
                .course(testCourse)
                .student(studentUser1)
                .status(EnrollmentStatus.ENROLLED)
                .build());

        authenticateAs(studentUser1, "STUDENT");

        CreateDiscussionThreadRequest request = CreateDiscussionThreadRequest.builder()
                .title("Câu hỏi về định lý Pytago")
                .content("Làm sao chứng minh tam giác vuông ạ?")
                .build();

        DiscussionThreadResponse thread = discussionService.createThread(testCourse.getId(), studentUser1.getId(), request);

        assertThat(thread).isNotNull();
        assertThat(thread.getTitle()).isEqualTo("Câu hỏi về định lý Pytago");
        assertThat(thread.getStatus()).isEqualTo(DiscussionThreadStatus.OPEN);
        assertThat(thread.getPostCount()).isEqualTo(0);

        PageResponse<DiscussionThreadResponse> threads = discussionService.getCourseThreads(
                testCourse.getId(), null, null, null, PageRequest.of(0, 10));
        assertThat(threads.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("3. Reply to thread, upvote and mark accepted answer workflow")
    void testRepliesAndMarkAnswerWorkflow() {
        enrollmentRepository.save(CourseEnrollment.builder()
                .course(testCourse)
                .student(studentUser1)
                .status(EnrollmentStatus.ENROLLED)
                .build());

        enrollmentRepository.save(CourseEnrollment.builder()
                .course(testCourse)
                .student(studentUser2)
                .status(EnrollmentStatus.ENROLLED)
                .build());

        // Student 1 creates thread
        authenticateAs(studentUser1, "STUDENT");
        DiscussionThreadResponse thread = discussionService.createThread(testCourse.getId(), studentUser1.getId(),
                CreateDiscussionThreadRequest.builder().title("Hỏi bài tập khó").content("Giúp mình câu 3 với.").build());

        // Student 2 replies
        authenticateAs(studentUser2, "STUDENT");
        DiscussionPostResponse reply = discussionService.createPost(testCourse.getId(), thread.getId(), studentUser2.getId(),
                CreateDiscussionPostRequest.builder().content("Bạn áp dụng công thức a^2 + b^2 = c^2 nhé.").build());

        assertThat(reply).isNotNull();
        assertThat(reply.getContent()).contains("a^2 + b^2 = c^2");
        assertThat(reply.getUpvoteCount()).isEqualTo(0);

        // Student 1 upvotes reply
        authenticateAs(studentUser1, "STUDENT");
        DiscussionPostResponse upvoted = discussionService.toggleUpvote(testCourse.getId(), thread.getId(), reply.getId(), studentUser1.getId());
        assertThat(upvoted.getUpvoteCount()).isEqualTo(1);
        assertThat(upvoted.getIsUpvotedByMe()).isTrue();

        // Student 1 toggles upvote again -> decreases
        DiscussionPostResponse unUpvoted = discussionService.toggleUpvote(testCourse.getId(), thread.getId(), reply.getId(), studentUser1.getId());
        assertThat(unUpvoted.getUpvoteCount()).isEqualTo(0);
        assertThat(unUpvoted.getIsUpvotedByMe()).isFalse();

        // Teacher marks student 2's reply as accepted answer
        authenticateAs(teacherUser, "TEACHER");
        DiscussionPostResponse answered = discussionService.markAnswer(testCourse.getId(), thread.getId(), reply.getId(), teacherUser.getId());
        assertThat(answered.getIsAnswer()).isTrue();

        // Thread status becomes RESOLVED
        DiscussionThread updatedThread = threadRepository.findById(thread.getId()).orElseThrow();
        assertThat(updatedThread.getStatus()).isEqualTo(DiscussionThreadStatus.RESOLVED);
    }

    @Test
    @DisplayName("4. Teacher pins, locks thread and prevents further replies")
    void testTeacher_PinAndLockThread() {
        enrollmentRepository.save(CourseEnrollment.builder()
                .course(testCourse)
                .student(studentUser1)
                .status(EnrollmentStatus.ENROLLED)
                .build());

        authenticateAs(studentUser1, "STUDENT");
        DiscussionThreadResponse thread = discussionService.createThread(testCourse.getId(), studentUser1.getId(),
                CreateDiscussionThreadRequest.builder().title("Thread cần khóa").content("Nội dung").build());

        // Teacher pins and locks
        authenticateAs(teacherUser, "TEACHER");
        discussionService.pinThread(testCourse.getId(), thread.getId(), teacherUser.getId(), true);
        discussionService.lockThread(testCourse.getId(), thread.getId(), teacherUser.getId(), true);

        DiscussionThread updatedThread = threadRepository.findById(thread.getId()).orElseThrow();
        assertThat(updatedThread.getIsPinned()).isTrue();
        assertThat(updatedThread.getIsLocked()).isTrue();

        // Student attempting to reply to locked thread is blocked
        authenticateAs(studentUser1, "STUDENT");
        assertThatThrownBy(() -> discussionService.createPost(testCourse.getId(), thread.getId(), studentUser1.getId(),
                CreateDiscussionPostRequest.builder().content("Thử gửi tin nhắn vào thread đã khóa").build()))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("đã bị khóa");
    }

    @Test
    @DisplayName("5. Course announcements creation and retrieval")
    void testCourseAnnouncements() {
        authenticateAs(teacherUser, "TEACHER");

        CreateCourseAnnouncementRequest request = CreateCourseAnnouncementRequest.builder()
                .title("Thông báo lịch thi giữa kỳ")
                .content("Kỳ thi sẽ bắt đầu vào tuần tới lúc 8h sáng.")
                .build();

        CourseAnnouncementResponse ann = announcementService.createAnnouncement(testCourse.getId(), teacherUser.getId(), request);
        assertThat(ann).isNotNull();
        assertThat(ann.getTitle()).isEqualTo("Thông báo lịch thi giữa kỳ");

        PageResponse<CourseAnnouncementResponse> list = announcementService.getCourseAnnouncements(testCourse.getId(), PageRequest.of(0, 10));
        assertThat(list.getItems()).hasSize(1);
        assertThat(list.getItems().get(0).getTitle()).isEqualTo("Thông báo lịch thi giữa kỳ");
    }

    @Test
    @DisplayName("6. Mention candidates retrieval and mention notifications")
    void testMentionCandidatesAndNotification() {
        enrollmentRepository.save(CourseEnrollment.builder()
                .course(testCourse)
                .student(studentUser1)
                .status(EnrollmentStatus.ENROLLED)
                .build());

        enrollmentRepository.save(CourseEnrollment.builder()
                .course(testCourse)
                .student(studentUser2)
                .status(EnrollmentStatus.ENROLLED)
                .build());

        // 1. Get mention candidates for the course
        List<MentionCandidateResponse> candidates = discussionService.getMentionCandidates(testCourse.getId());
        assertThat(candidates).isNotEmpty();
        List<UUID> candidateIds = candidates.stream().map(MentionCandidateResponse::getId).toList();
        assertThat(candidateIds).contains(teacherUser.getId(), studentUser1.getId(), studentUser2.getId());

        // 2. Student 1 creates a thread mentioning Teacher and Student 2
        authenticateAs(studentUser1, "STUDENT");
        DiscussionThreadResponse thread = discussionService.createThread(testCourse.getId(), studentUser1.getId(),
                CreateDiscussionThreadRequest.builder()
                        .title("Hỏi bài có tag @Thầy")
                        .content("Em xin hỏi @Thầy và @Bạn 2")
                        .mentionedUserIds(List.of(teacherUser.getId(), studentUser2.getId()))
                        .build());

        assertThat(thread).isNotNull();

        // 3. Student 2 replies mentioning Student 1
        authenticateAs(studentUser2, "STUDENT");
        DiscussionPostResponse reply = discussionService.createPost(testCourse.getId(), thread.getId(), studentUser2.getId(),
                CreateDiscussionPostRequest.builder()
                        .content("@Học Viên 1 em đồng ý kiến")
                        .mentionedUserIds(List.of(studentUser1.getId()))
                        .build());

        assertThat(reply).isNotNull();
        assertThat(reply.getParentId()).isNull();

        // 4. Student 1 replies to Student 2's reply (nested reply)
        authenticateAs(studentUser1, "STUDENT");
        DiscussionPostResponse nestedReply = discussionService.createPost(testCourse.getId(), thread.getId(), studentUser1.getId(),
                CreateDiscussionPostRequest.builder()
                        .parentId(reply.getId())
                        .content("@Học Viên 2 cảm ơn bạn nhé")
                        .mentionedUserIds(List.of(studentUser2.getId()))
                        .build());

        assertThat(nestedReply).isNotNull();
        assertThat(nestedReply.getParentId()).isEqualTo(reply.getId());
    }
}
