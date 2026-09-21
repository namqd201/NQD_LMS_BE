package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.teacher.TeacherExamRequest;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class TeacherExamCrudAndSmartQuestionSelectionTest {

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
    private QuestionRepository questionRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private ExamAssignmentRepository examAssignmentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private User teacher1;
    private User teacher2;
    private Subject mathSubject;
    private Question q1MathGrade1;
    private Question q2MathGrade1;
    private Question q3MathGrade2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role teacherRole = roleRepository.findByName("ROLE_TEACHER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_TEACHER").description("Teacher").build()));

        teacher1 = userRepository.findByEmail("teacher1_exam@nqd.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("teacher1_exam@nqd.edu.vn")
                        .fullName("Teacher One Exam")
                        .status(UserStatus.ACTIVE)
                        .build()));
        if (userRoleRepository.findByUserId(teacher1.getId()).isEmpty()) {
            userRoleRepository.save(UserRole.builder().userId(teacher1.getId()).roleId(teacherRole.getId()).build());
        }

        teacher2 = userRepository.findByEmail("teacher2_exam@nqd.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("teacher2_exam@nqd.edu.vn")
                        .fullName("Teacher Two Exam")
                        .status(UserStatus.ACTIVE)
                        .build()));
        if (userRoleRepository.findByUserId(teacher2.getId()).isEmpty()) {
            userRoleRepository.save(UserRole.builder().userId(teacher2.getId()).roleId(teacherRole.getId()).build());
        }

        mathSubject = subjectRepository.findByCode("MATH_EXAM_TEST")
                .orElseGet(() -> subjectRepository.save(Subject.builder()
                        .name("Toán học")
                        .code("MATH_EXAM_TEST")
                        .status(SubjectStatus.ACTIVE)
                        .build()));

        // Question 1: Math - Grade 1
        q1MathGrade1 = questionRepository.save(Question.builder()
                .subject(mathSubject)
                .gradeLevel("Lớp 1")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.EASY)
                .content("1 + 1 bằng mấy?")
                .defaultMarks(new BigDecimal("1.00"))
                .status(QuestionStatus.APPROVED)
                .creator(teacher1)
                .build());

        // Question 2: Math - Grade 1
        q2MathGrade1 = questionRepository.save(Question.builder()
                .subject(mathSubject)
                .gradeLevel("Lớp 1")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.EASY)
                .content("2 + 2 bằng mấy?")
                .defaultMarks(new BigDecimal("1.00"))
                .status(QuestionStatus.APPROVED)
                .creator(teacher1)
                .build());

        // Question 3: Math - Grade 2
        q3MathGrade2 = questionRepository.save(Question.builder()
                .subject(mathSubject)
                .gradeLevel("Lớp 2")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.MEDIUM)
                .content("10 - 7 bằng mấy?")
                .defaultMarks(new BigDecimal("1.00"))
                .status(QuestionStatus.APPROVED)
                .creator(teacher1)
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
    @DisplayName("Teacher can query questions filtered specifically by Subject and Grade Level")
    void testQuestionFilterBySubjectAndGradeLevel() throws Exception {
        OAuth2AuthenticationToken teacherAuth = createAuthToken(teacher1, Set.of("TEACHER"));

        // Filter Math + Grade 1 -> should return 2 questions (q1, q2)
        mockMvc.perform(get("/api/v1/teacher/questions")
                        .param("subjectId", mathSubject.getId().toString())
                        .param("gradeLevel", "Lớp 1")
                        .with(authentication(teacherAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].content", containsInAnyOrder("1 + 1 bằng mấy?", "2 + 2 bằng mấy?")));

        // Filter Math + Grade 2 -> should return 1 question (q3)
        mockMvc.perform(get("/api/v1/teacher/questions")
                        .param("subjectId", mathSubject.getId().toString())
                        .param("gradeLevel", "Lớp 2")
                        .with(authentication(teacherAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content", is("10 - 7 bằng mấy?")));
    }

    @Test
    @DisplayName("Teacher can create exam with rule-based code generation and selected question ids")
    void testTeacherCreateExamWithRuleCodeAndQuestions() throws Exception {
        OAuth2AuthenticationToken teacherAuth = createAuthToken(teacher1, Set.of("TEACHER"));

        TeacherExamRequest request = TeacherExamRequest.builder()
                .subjectId(mathSubject.getId())
                .gradeLevel("Lớp 1")
                .title("Kiểm tra đánh giá năng lực môn Toán Lớp 1")
                .description("Đề kiểm tra trắc nghiệm 45 phút")
                .instructions("Không mở sách vở khi làm bài")
                .durationMinutes(45)
                .totalMarks(new BigDecimal("10.00"))
                .passingMarks(new BigDecimal("5.00"))
                .questionIds(List.of(q1MathGrade1.getId(), q2MathGrade1.getId()))
                .build();

        mockMvc.perform(post("/api/v1/teacher/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(teacherAuth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title", is("Kiểm tra đánh giá năng lực môn Toán Lớp 1")))
                .andExpect(jsonPath("$.code", startsWith("DE_TOAN_L1_")))
                .andExpect(jsonPath("$.gradeLevel", is("Lớp 1")))
                .andExpect(jsonPath("$.questionCount", is(2)))
                .andExpect(jsonPath("$.questions", hasSize(2)));
    }

    @Test
    @DisplayName("Teacher can publish and archive exam; another teacher cannot modify")
    void testExamPublishArchiveAndOwnershipSecurity() throws Exception {
        OAuth2AuthenticationToken teacher1Auth = createAuthToken(teacher1, Set.of("TEACHER"));
        OAuth2AuthenticationToken teacher2Auth = createAuthToken(teacher2, Set.of("TEACHER"));

        // Teacher 1 creates an exam
        Exam exam = examRepository.save(Exam.builder()
                .subject(mathSubject)
                .gradeLevel("Lớp 1")
                .code("DE_TOAN_L1_9999")
                .title("Đề thi Toán 1")
                .durationMinutes(30)
                .totalMarks(new BigDecimal("10.00"))
                .passingMarks(new BigDecimal("5.00"))
                .status(ExamStatus.DRAFT)
                .creator(teacher1)
                .build());

        // Teacher 1 publishes the exam
        mockMvc.perform(put("/api/v1/teacher/exams/" + exam.getId() + "/publish")
                        .with(authentication(teacher1Auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PUBLISHED")));

        // Teacher 1 archives the exam
        mockMvc.perform(put("/api/v1/teacher/exams/" + exam.getId() + "/archive")
                        .with(authentication(teacher1Auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ARCHIVED")));

        // Teacher 2 attempts to delete Teacher 1's exam -> 403 Forbidden
        mockMvc.perform(delete("/api/v1/teacher/exams/" + exam.getId())
                        .with(authentication(teacher2Auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Teacher can create university exam with Năm nhất generating N1 code suffix")
    void testUniversityExamCodeGeneration() throws Exception {
        OAuth2AuthenticationToken teacherAuth = createAuthToken(teacher1, Set.of("TEACHER"));

        TeacherExamRequest request = TeacherExamRequest.builder()
                .subjectId(mathSubject.getId())
                .gradeLevel("Năm nhất")
                .title("Đề thi Toán cao cấp A1 - Năm nhất Đại học")
                .durationMinutes(90)
                .totalMarks(new BigDecimal("10.00"))
                .passingMarks(new BigDecimal("5.00"))
                .build();

        mockMvc.perform(post("/api/v1/teacher/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(teacherAuth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", startsWith("DE_TOAN_N1_")))
                .andExpect(jsonPath("$.gradeLevel", is("Năm nhất")));
    }

    @Test
    @DisplayName("Teacher can assign enrolled students to exam, which are visible to students and send notifications")
    void testAssignStudentsToExamAndStudentSeesAssignedExam() throws Exception {
        OAuth2AuthenticationToken teacherAuth = createAuthToken(teacher1, Set.of("TEACHER"));

        // Create student
        User student = userRepository.findByEmail("student_exam_assign@nqd.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("student_exam_assign@nqd.edu.vn")
                        .fullName("Student Exam Assignee")
                        .status(UserStatus.ACTIVE)
                        .build()));
        OAuth2AuthenticationToken studentAuth = createAuthToken(student, Set.of("STUDENT"));

        // Create course
        Course course = courseRepository.save(Course.builder()
                .subject(mathSubject)
                .name("Toán Lớp 1 Nâng Cao")
                .code("MATH1_ADV")
                .gradeLevel("Lớp 1")
                .status(CourseStatus.ACTIVE)
                .creator(teacher1)
                .build());

        // Enroll student in course
        courseEnrollmentRepository.save(CourseEnrollment.builder()
                .course(course)
                .student(student)
                .status(EnrollmentStatus.ENROLLED)
                .build());

        // Create exam for course
        Exam exam = examRepository.save(Exam.builder()
                .course(course)
                .subject(mathSubject)
                .title("Kiểm tra giữa kỳ Toán 1")
                .code("DE_TOAN_L1_TEST")
                .gradeLevel("Lớp 1")
                .durationMinutes(45)
                .totalMarks(new BigDecimal("10.00"))
                .passingMarks(new BigDecimal("5.00"))
                .status(ExamStatus.PUBLISHED)
                .creator(teacher1)
                .build());

        // 1. Teacher gets eligible students list
        mockMvc.perform(get("/api/v1/teacher/exams/" + exam.getId() + "/eligible-students")
                        .with(authentication(teacherAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentEmail", is("student_exam_assign@nqd.edu.vn")))
                .andExpect(jsonPath("$[0].isAssigned", is(false)));

        // 2. Teacher assigns the student to the exam
        Map<String, Object> assignPayload = Map.of("studentIds", List.of(student.getId().toString()));
        mockMvc.perform(post("/api/v1/teacher/exams/" + exam.getId() + "/assign-students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignPayload))
                        .with(authentication(teacherAuth)))
                .andExpect(status().isOk());

        // 3. Teacher verifies student is now marked as assigned
        mockMvc.perform(get("/api/v1/teacher/exams/" + exam.getId() + "/eligible-students")
                        .with(authentication(teacherAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isAssigned", is(true)));

        // 4. Student queries assigned exams and finds this exam
        mockMvc.perform(get("/api/v1/student/exams/my-assigned-exams")
                        .with(authentication(studentAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.examId == '" + exam.getId() + "')].title", contains("Kiểm tra giữa kỳ Toán 1")));
    }
}
