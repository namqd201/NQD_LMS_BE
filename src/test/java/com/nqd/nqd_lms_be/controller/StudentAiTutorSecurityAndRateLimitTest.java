package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.ai.StudentAiRateLimiter;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.student.StudentAiTutorMode;
import com.nqd.nqd_lms_be.dto.student.StudentAiTutorRequest;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class StudentAiTutorSecurityAndRateLimitTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private CourseEnrollmentRepository enrollmentRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionOptionRepository questionOptionRepository;

    @Autowired
    private ExamAnswerRepository examAnswerRepository;

    @Autowired
    private StudentAiRateLimiter rateLimiter;

    @MockitoBean
    private com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService membershipEntitlementService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private User student1;
    private User student2;
    private Course course;
    private Lesson publishedLesson;
    private Lesson draftLesson;
    private Exam exam;
    private ExamAttempt student1Attempt;
    private ExamAttempt student2Attempt;
    private Question question;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("STUDENT").description("Student Role").build()));

        student1 = userRepository.save(User.builder()
                .email("student1_ai_tutor@test.com")
                .fullName("Student One")
                .status(UserStatus.ACTIVE)
                .build());
        userRoleRepository.save(UserRole.builder().userId(student1.getId()).roleId(studentRole.getId()).build());

        student2 = userRepository.save(User.builder()
                .email("student2_ai_tutor@test.com")
                .fullName("Student Two")
                .status(UserStatus.ACTIVE)
                .build());
        userRoleRepository.save(UserRole.builder().userId(student2.getId()).roleId(studentRole.getId()).build());

        Subject subject = subjectRepository.save(Subject.builder()
                .code("MATH_AI_TUTOR_" + UUID.randomUUID().toString().substring(0, 8))
                .name("Toán Học AI")
                .status(SubjectStatus.ACTIVE)
                .build());

        course = courseRepository.save(Course.builder()
                .name("Toán 1 Nâng Cao")
                .code("MATH101_" + UUID.randomUUID().toString().substring(0, 8))
                .subject(subject)
                .gradeLevel("Lớp 1")
                .status(CourseStatus.ACTIVE)
                .build());

        enrollmentRepository.save(CourseEnrollment.builder()
                .course(course)
                .student(student1)
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .build());

        Chapter chapter = chapterRepository.save(Chapter.builder()
                .course(course)
                .title("Chương 1: Phép Cộng Trừ")
                .displayOrder(1)
                .build());

        publishedLesson = lessonRepository.save(Lesson.builder()
                .chapter(chapter)
                .title("Bài 1: Phép cộng trong phạm vi 10")
                .summary("Quy tắc tính toán cộng trong phạm vi 10 cơ bản")
                .content("Nội dung bài học: 1 + 1 = 2, 2 + 2 = 4, 5 + 5 = 10...")
                .displayOrder(1)
                .status(LessonStatus.PUBLISHED)
                .build());

        draftLesson = lessonRepository.save(Lesson.builder()
                .chapter(chapter)
                .title("Bài 2 (Bản Nháp Chưa Xuất Bản)")
                .summary("Bản nháp bí mật của giáo viên")
                .content("Nội dung chưa công bố...")
                .displayOrder(2)
                .status(LessonStatus.DRAFT)
                .build());

        exam = examRepository.save(Exam.builder()
                .title("Kiểm Tra Giữa Kỳ Toán 1")
                .code("EXAM_MATH1_" + UUID.randomUUID().toString().substring(0, 8))
                .subject(subject)
                .gradeLevel("Lớp 1")
                .status(ExamStatus.PUBLISHED)
                .durationMinutes(15)
                .totalMarks(new BigDecimal("10.00"))
                .passingMarks(new BigDecimal("5.00"))
                .maxAttempts(3)
                .build());

        question = questionRepository.save(Question.builder()
                .subject(subject)
                .gradeLevel("Lớp 1")
                .content("Kết quả của 7 + 2 là bao nhiêu?")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.EASY)
                .defaultMarks(new BigDecimal("2.00"))
                .explanation("7 + 2 = 9. Ta đếm thêm 2 đơn vị từ 7.")
                .status(QuestionStatus.APPROVED)
                .build());

        QuestionOption optA = questionOptionRepository.save(QuestionOption.builder().question(question).optionKey("A").optionText("8").isCorrect(false).displayOrder(1).build());
        QuestionOption optB = questionOptionRepository.save(QuestionOption.builder().question(question).optionKey("B").optionText("9").isCorrect(true).displayOrder(2).build());
        QuestionOption optC = questionOptionRepository.save(QuestionOption.builder().question(question).optionKey("C").optionText("10").isCorrect(false).displayOrder(3).build());

        student1Attempt = examAttemptRepository.save(ExamAttempt.builder()
                .exam(exam)
                .student(student1)
                .attemptNumber(1)
                .status(ExamAttemptStatus.SUBMITTED)
                .startedAt(LocalDateTime.now().minusMinutes(10))
                .submittedAt(LocalDateTime.now())
                .totalScore(new BigDecimal("0.00"))
                .passed(false)
                .build());

        examAnswerRepository.save(ExamAnswer.builder()
                .attempt(student1Attempt)
                .question(question)
                .selectedOption(optA) // Wrong option
                .isCorrect(false)
                .maxMarks(new BigDecimal("2.00"))
                .marksAwarded(BigDecimal.ZERO)
                .gradingStatus(GradingStatus.AUTO_GRADED)
                .build());

        student2Attempt = examAttemptRepository.save(ExamAttempt.builder()
                .exam(exam)
                .student(student2)
                .attemptNumber(1)
                .status(ExamAttemptStatus.SUBMITTED)
                .startedAt(LocalDateTime.now().minusMinutes(10))
                .submittedAt(LocalDateTime.now())
                .totalScore(new BigDecimal("2.00"))
                .passed(true)
                .build());

        rateLimiter.resetForStudent(student1.getId());
        rateLimiter.resetForStudent(student2.getId());
    }

    private OAuth2AuthenticationToken createAuthToken(User user, String roleName) {
        AppUserPrincipal principal = new AppUserPrincipal(
                user,
                Set.of(roleName),
                Map.of("sub", "sub-" + user.getId(), "email", user.getEmail(), "name", user.getFullName()),
                null,
                null
        );
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    @Test
    @DisplayName("Student can ask general question to AI Tutor")
    void testAskGeneralQuestion() throws Exception {
        StudentAiTutorRequest req = StudentAiTutorRequest.builder()
                .mode(StudentAiTutorMode.GENERAL_QA)
                .question("Làm thế nào để cộng nhẩm nhanh trong phạm vi 10?")
                .build();

        mockMvc.perform(post("/api/v1/student/ai-tutor/ask")
                        .with(authentication(createAuthToken(student1, "STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", notNullValue()))
                .andExpect(jsonPath("$.mode", is("GENERAL_QA")));
    }

    @Test
    @DisplayName("Student can ask AI Tutor to explain a published lesson")
    void testExplainPublishedLesson() throws Exception {
        mockMvc.perform(post("/api/v1/student/ai-tutor/explain-lesson/" + publishedLesson.getId())
                        .with(authentication(createAuthToken(student1, "STUDENT")))
                        .param("customQuestion", "Hãy giải thích trọng tâm bài học này"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", notNullValue()))
                .andExpect(jsonPath("$.contextSummary", containsString("Bài 1")));
    }

    @Test
    @DisplayName("Student is FORBIDDEN from asking AI Tutor about an unpublished DRAFT lesson")
    void testAccessDeniedForDraftLesson() throws Exception {
        mockMvc.perform(post("/api/v1/student/ai-tutor/explain-lesson/" + draftLesson.getId())
                        .with(authentication(createAuthToken(student1, "STUDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Student can ask AI Tutor why their submitted answer was wrong")
    void testExplainWrongAnswer() throws Exception {
        mockMvc.perform(post("/api/v1/student/ai-tutor/explain-answer")
                        .with(authentication(createAuthToken(student1, "STUDENT")))
                        .param("examAttemptId", student1Attempt.getId().toString())
                        .param("questionId", question.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", notNullValue()))
                .andExpect(jsonPath("$.contextSummary", containsString("Kết quả của 7 + 2")));
    }

    @Test
    @DisplayName("Student is FORBIDDEN from asking AI about another student's exam attempt")
    void testAccessDeniedForOtherStudentAttempt() throws Exception {
        mockMvc.perform(post("/api/v1/student/ai-tutor/explain-answer")
                        .with(authentication(createAuthToken(student1, "STUDENT")))
                        .param("examAttemptId", student2Attempt.getId().toString())
                        .param("questionId", question.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Student can ask AI Tutor for a hint without revealing answers")
    void testProvideHint() throws Exception {
        mockMvc.perform(post("/api/v1/student/ai-tutor/hint")
                        .with(authentication(createAuthToken(student1, "STUDENT")))
                        .param("examAttemptId", student1Attempt.getId().toString())
                        .param("questionId", question.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hint", notNullValue()));
    }

    @Test
    @DisplayName("Student can get personalized study recommendations")
    void testGetStudyRecommendations() throws Exception {
        mockMvc.perform(get("/api/v1/student/ai-tutor/recommendations")
                        .with(authentication(createAuthToken(student1, "STUDENT")))
                        .param("courseId", course.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations", notNullValue()));
    }

    @Test
    @DisplayName("Rate Limiter blocks student requests when exceeding threshold (HTTP 429)")
    void testRateLimiterEnforcement() throws Exception {
        StudentAiTutorRequest req = StudentAiTutorRequest.builder()
                .mode(StudentAiTutorMode.GENERAL_QA)
                .question("Test rate limit")
                .build();

        // Perform 20 requests (within quota)
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/student/ai-tutor/ask")
                            .with(authentication(createAuthToken(student1, "STUDENT")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        // 21st request should exceed limit and return 429 Too Many Requests
        mockMvc.perform(post("/api/v1/student/ai-tutor/ask")
                        .with(authentication(createAuthToken(student1, "STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("vượt quá giới hạn")));
    }
}
