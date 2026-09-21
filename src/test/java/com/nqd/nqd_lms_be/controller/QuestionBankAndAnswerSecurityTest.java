package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.teacher.TeacherQuestionOptionDto;
import com.nqd.nqd_lms_be.dto.teacher.TeacherQuestionRequest;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class QuestionBankAndAnswerSecurityTest {

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
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionOptionRepository questionOptionRepository;

    @Autowired
    private QuestionTagRepository questionTagRepository;

    @Autowired
    private QuestionTagRelationRepository questionTagRelationRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamQuestionRepository examQuestionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private User teacher1;
    private User teacher2;
    private User student1;
    private Subject subject;
    private Course course;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role teacherRole = roleRepository.findByName("ROLE_TEACHER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_TEACHER").description("Teacher").build()));

        Role studentRole = roleRepository.findByName("ROLE_STUDENT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_STUDENT").description("Student").build()));

        teacher1 = userRepository.save(User.builder()
                .email("teacher1_qb@nqd.edu.vn")
                .fullName("Teacher One")
                .status(UserStatus.ACTIVE)
                .build());
        userRoleRepository.save(UserRole.builder().userId(teacher1.getId()).roleId(teacherRole.getId()).build());

        teacher2 = userRepository.save(User.builder()
                .email("teacher2_qb@nqd.edu.vn")
                .fullName("Teacher Two")
                .status(UserStatus.ACTIVE)
                .build());
        userRoleRepository.save(UserRole.builder().userId(teacher2.getId()).roleId(teacherRole.getId()).build());

        student1 = userRepository.save(User.builder()
                .email("student1_qb@nqd.edu.vn")
                .fullName("Student One")
                .status(UserStatus.ACTIVE)
                .build());
        userRoleRepository.save(UserRole.builder().userId(student1.getId()).roleId(studentRole.getId()).build());

        subject = subjectRepository.save(Subject.builder()
                .name("Toán học 12")
                .code("MATH12")
                .status(SubjectStatus.ACTIVE)
                .build());

        course = courseRepository.save(Course.builder()
                .subject(subject)
                .creator(teacher1)
                .name("Giải tích 12 Nâng cao")
                .code("MATH12_ADV")
                .status(CourseStatus.ACTIVE)
                .build());

        Chapter chapter = chapterRepository.save(Chapter.builder()
                .course(course)
                .title("Chương 1: Khảo sát hàm số")
                .displayOrder(1)
                .build());

        lesson = lessonRepository.save(Lesson.builder()
                .chapter(chapter)
                .title("Bài 1: Tính đơn điệu của hàm số")
                .displayOrder(1)
                .status(LessonStatus.PUBLISHED)
                .build());
    }

    private OAuth2AuthenticationToken createAuthToken(User user, Set<String> roles) {
        AppUserPrincipal principal = new AppUserPrincipal(
                user,
                roles,
                Map.of("sub", "sub-" + user.getId(), "email", user.getEmail()),
                null,
                null
        );
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    @Test
    @DisplayName("Teacher can create, update, archive and view full question with explanation and answer keys")
    void testTeacherQuestionBankCrudAndArchive() throws Exception {
        OAuth2AuthenticationToken teacherAuth = createAuthToken(teacher1, Set.of("TEACHER"));

        TeacherQuestionRequest request = TeacherQuestionRequest.builder()
                .subjectId(subject.getId())
                .courseId(course.getId())
                .lessonId(lesson.getId())
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.MEDIUM)
                .content("Hàm số y = x^3 - 3x đồng biến trên khoảng nào?")
                .explanation("Đạo hàm y' = 3x^2 - 3 > 0 khi x < -1 hoặc x > 1.")
                .defaultMarks(new BigDecimal("2.00"))
                .status(QuestionStatus.DRAFT)
                .tags(List.of("GiaiTich", "DaoHam", "KhaoSatHamSo"))
                .options(List.of(
                        TeacherQuestionOptionDto.builder().optionKey("A").optionText("(-1; 1)").isCorrect(false).displayOrder(1).build(),
                        TeacherQuestionOptionDto.builder().optionKey("B").optionText("(1; +vô cùng)").isCorrect(true).displayOrder(2).build(),
                        TeacherQuestionOptionDto.builder().optionKey("C").optionText("(-vô cùng; 0)").isCorrect(false).displayOrder(3).build(),
                        TeacherQuestionOptionDto.builder().optionKey("D").optionText("(0; 2)").isCorrect(false).displayOrder(4).build()
                ))
                .build();

        // 1. Create Question
        MvcResult createResult = mockMvc.perform(post("/api/v1/teacher/questions")
                        .with(authentication(teacherAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.subjectId").value(subject.getId().toString()))
                .andExpect(jsonPath("$.content").value("Hàm số y = x^3 - 3x đồng biến trên khoảng nào?"))
                .andExpect(jsonPath("$.explanation").value("Đạo hàm y' = 3x^2 - 3 > 0 khi x < -1 hoặc x > 1."))
                .andExpect(jsonPath("$.tags", hasItems("GiaiTich", "DaoHam", "KhaoSatHamSo")))
                .andExpect(jsonPath("$.options", hasSize(4)))
                .andExpect(jsonPath("$.options[1].isCorrect").value(true))
                .andReturn();

        String responseJson = createResult.getResponse().getContentAsString();
        String questionIdStr = objectMapper.readTree(responseJson).get("id").asText();
        UUID questionId = UUID.fromString(questionIdStr);

        // 2. Archive Question
        mockMvc.perform(put("/api/v1/teacher/questions/" + questionId + "/archive")
                        .with(authentication(teacherAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(questionIdStr))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        // 3. Search and filter
        mockMvc.perform(get("/api/v1/teacher/questions")
                        .param("subjectId", subject.getId().toString())
                        .param("status", "ARCHIVED")
                        .param("tag", "DaoHam")
                        .with(authentication(teacherAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(questionIdStr));
    }

    @Test
    @DisplayName("Teacher cannot modify or delete questions created by another teacher")
    void testTeacherCannotModifyAnotherTeacherQuestion() throws Exception {
        OAuth2AuthenticationToken teacher2Auth = createAuthToken(teacher2, Set.of("TEACHER"));

        // Teacher 1 creates a question
        Question q1 = questionRepository.save(Question.builder()
                .subject(subject)
                .creator(teacher1)
                .questionType(QuestionType.SHORT_ANSWER)
                .difficulty(QuestionDifficulty.HARD)
                .content("Tìm giá trị nhỏ nhất của f(x)")
                .explanation("Lời giải mật của giáo viên 1")
                .defaultMarks(new BigDecimal("1.00"))
                .status(QuestionStatus.DRAFT)
                .build());

        TeacherQuestionRequest updateReq = TeacherQuestionRequest.builder()
                .subjectId(subject.getId())
                .questionType(QuestionType.SHORT_ANSWER)
                .difficulty(QuestionDifficulty.HARD)
                .content("Hacked content")
                .build();

        // Teacher 2 attempts update -> Forbidden (403)
        mockMvc.perform(put("/api/v1/teacher/questions/" + q1.getId())
                        .with(authentication(teacher2Auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // Teacher 2 attempts delete -> Forbidden (403)
        mockMvc.perform(delete("/api/v1/teacher/questions/" + q1.getId())
                        .with(authentication(teacher2Auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CRITICAL SECURITY: Student cannot create or delete questions in Teacher Question Bank")
    void testStudentCannotAccessQuestionBank() throws Exception {
        OAuth2AuthenticationToken studentAuth = createAuthToken(student1, Set.of("STUDENT"));

        mockMvc.perform(delete("/api/v1/teacher/questions/" + UUID.randomUUID())
                        .with(authentication(studentAuth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CRITICAL SECURITY: Student taking exam NEVER receives correctAnswer, isCorrect, or explanation")
    void testStudentTakingExamNeverReceivesAnswerOrExplanation() throws Exception {
        OAuth2AuthenticationToken studentAuth = createAuthToken(student1, Set.of("STUDENT"));

        // 1. Create a published Exam with questions and options
        Exam exam = examRepository.save(Exam.builder()
                .subject(subject)
                .creator(teacher1)
                .title("Đề kiểm tra 1 tiết Đại số 12")
                .durationMinutes(45)
                .totalMarks(new BigDecimal("10.00"))
                .passingMarks(new BigDecimal("5.00"))
                .status(ExamStatus.PUBLISHED)
                .build());

        Question q = questionRepository.save(Question.builder()
                .subject(subject)
                .creator(teacher1)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.MEDIUM)
                .content("Giá trị cực đại của hàm số y = -x^2 + 4x + 1 là:")
                .explanation("TOP SECRET: f'(x) = -2x + 4 = 0 => x = 2, f(2) = 5.")
                .defaultMarks(new BigDecimal("5.00"))
                .status(QuestionStatus.APPROVED)
                .build());

        questionOptionRepository.save(QuestionOption.builder()
                .question(q)
                .optionKey("A")
                .optionText("5")
                .isCorrect(true)
                .displayOrder(1)
                .build());

        questionOptionRepository.save(QuestionOption.builder()
                .question(q)
                .optionKey("B")
                .optionText("4")
                .isCorrect(false)
                .displayOrder(2)
                .build());

        examQuestionRepository.save(ExamQuestion.builder()
                .exam(exam)
                .question(q)
                .marks(new BigDecimal("5.00"))
                .displayOrder(1)
                .build());

        // 2. Student starts exam
        MvcResult result = mockMvc.perform(post("/api/v1/student/exams/" + exam.getId() + "/start")
                        .with(authentication(studentAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").isNotEmpty())
                .andExpect(jsonPath("$.questions", hasSize(1)))
                .andExpect(jsonPath("$.questions[0].content").value("Giá trị cực đại của hàm số y = -x^2 + 4x + 1 là:"))
                .andExpect(jsonPath("$.questions[0].options", hasSize(2)))
                // CRITICAL ASSERTIONS: No answer leakage
                .andExpect(jsonPath("$.questions[0].explanation").doesNotExist())
                .andExpect(jsonPath("$.questions[0].correctAnswer").doesNotExist())
                .andExpect(jsonPath("$.questions[0].isCorrect").doesNotExist())
                .andExpect(jsonPath("$.questions[0].options[0].isCorrect").doesNotExist())
                .andExpect(jsonPath("$.questions[0].options[1].isCorrect").doesNotExist())
                .andReturn();

        // 3. String content verification: Ensure sensitive keywords are absent
        String json = result.getResponse().getContentAsString();
        assertThat(json).doesNotContain("isCorrect");
        assertThat(json).doesNotContain("is_correct");
        assertThat(json).doesNotContain("explanation");
        assertThat(json).doesNotContain("TOP SECRET");
    }

    @Test
    @DisplayName("Teacher can update question multiple times without duplicate key violation on options")
    void testUpdateQuestionWithOptionsDoesNotViolateUniqueConstraint() throws Exception {
        OAuth2AuthenticationToken teacherAuth = createAuthToken(teacher1, Set.of("TEACHER"));

        // 1. Create a Short Answer question with optionKey 'ANS'
        TeacherQuestionRequest createReq = TeacherQuestionRequest.builder()
                .subjectId(subject.getId())
                .questionType(QuestionType.SHORT_ANSWER)
                .difficulty(QuestionDifficulty.EASY)
                .content("Thủ đô của Việt Nam là gì?")
                .explanation("Hà Nội là thủ đô")
                .defaultMarks(new BigDecimal("1.00"))
                .status(QuestionStatus.DRAFT)
                .options(List.of(
                        TeacherQuestionOptionDto.builder().optionKey("ANS").optionText("Hà Nội").isCorrect(true).displayOrder(1).build()
                ))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/teacher/questions")
                        .with(authentication(teacherAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String questionIdStr = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();
        UUID questionId = UUID.fromString(questionIdStr);

        // 2. Update Question with new option text keeping same optionKey 'ANS'
        TeacherQuestionRequest updateReq = TeacherQuestionRequest.builder()
                .subjectId(subject.getId())
                .questionType(QuestionType.SHORT_ANSWER)
                .difficulty(QuestionDifficulty.EASY)
                .content("Thủ đô nước CHXHCN Việt Nam là gì?")
                .explanation("Hà Nội")
                .defaultMarks(new BigDecimal("2.00"))
                .status(QuestionStatus.APPROVED)
                .options(List.of(
                        TeacherQuestionOptionDto.builder().optionKey("ANS").optionText("Thủ đô Hà Nội").isCorrect(true).displayOrder(1).build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/teacher/questions/" + questionId)
                        .with(authentication(teacherAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Thủ đô nước CHXHCN Việt Nam là gì?"))
                .andExpect(jsonPath("$.options", hasSize(1)))
                .andExpect(jsonPath("$.options[0].optionKey").value("ANS"))
                .andExpect(jsonPath("$.options[0].optionText").value("Thủ đô Hà Nội"));
    }
}
