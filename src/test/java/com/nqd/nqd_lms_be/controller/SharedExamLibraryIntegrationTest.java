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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class SharedExamLibraryIntegrationTest {

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
    private ExamQuestionRepository examQuestionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private User teacher1;
    private User teacher2;
    private User admin;
    private Subject mathSubject;
    private Question q1;
    private Question q2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role teacherRole = roleRepository.findByName("ROLE_TEACHER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_TEACHER").description("Teacher").build()));
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").description("Admin").build()));

        teacher1 = userRepository.findByEmail("t1_shared_exam@nqd.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("t1_shared_exam@nqd.edu.vn")
                        .fullName("Teacher 1 Shared Exam")
                        .status(UserStatus.ACTIVE)
                        .build()));
        if (userRoleRepository.findByUserId(teacher1.getId()).isEmpty()) {
            userRoleRepository.save(UserRole.builder().userId(teacher1.getId()).roleId(teacherRole.getId()).build());
        }

        teacher2 = userRepository.findByEmail("t2_shared_exam@nqd.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("t2_shared_exam@nqd.edu.vn")
                        .fullName("Teacher 2 Shared Exam")
                        .status(UserStatus.ACTIVE)
                        .build()));
        if (userRoleRepository.findByUserId(teacher2.getId()).isEmpty()) {
            userRoleRepository.save(UserRole.builder().userId(teacher2.getId()).roleId(teacherRole.getId()).build());
        }

        admin = userRepository.findByEmail("admin_shared_exam@nqd.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("admin_shared_exam@nqd.edu.vn")
                        .fullName("Admin Shared Exam")
                        .status(UserStatus.ACTIVE)
                        .build()));
        if (userRoleRepository.findByUserId(admin.getId()).isEmpty()) {
            userRoleRepository.save(UserRole.builder().userId(admin.getId()).roleId(adminRole.getId()).build());
        }

        mathSubject = subjectRepository.findByCode("MATH_SHARED")
                .orElseGet(() -> subjectRepository.save(Subject.builder()
                        .name("Toán Học Shared")
                        .code("MATH_SHARED")
                        .description("Môn Toán dùng chung")
                        .status(SubjectStatus.ACTIVE)
                        .build()));

        q1 = questionRepository.save(Question.builder()
                .subject(mathSubject)
                .gradeLevel("Lớp 10")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.EASY)
                .content("1 + 1 = ?")
                .defaultMarks(new BigDecimal("1.00"))
                .status(QuestionStatus.APPROVED)
                .creator(teacher1)
                .build());

        q2 = questionRepository.save(Question.builder()
                .subject(mathSubject)
                .gradeLevel("Lớp 10")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.MEDIUM)
                .content("2 * 2 = ?")
                .defaultMarks(new BigDecimal("1.00"))
                .status(QuestionStatus.APPROVED)
                .creator(teacher1)
                .build());
    }

    private OAuth2AuthenticationToken createAuthToken(User user, String role) {
        AppUserPrincipal principal = new AppUserPrincipal(
                user,
                Set.of(role),
                Map.of("sub", "sub-" + user.getId(), "email", user.getEmail()),
                null,
                null
        );
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    @Test
    @DisplayName("Should create exam with visibility, update visibility, view in shared library, and clone successfully")
    void testSharedExamLibraryAndCloneWorkflow() throws Exception {
        OAuth2AuthenticationToken authTeacher1 = createAuthToken(teacher1, "TEACHER");
        OAuth2AuthenticationToken authTeacher2 = createAuthToken(teacher2, "TEACHER");
        OAuth2AuthenticationToken authAdmin = createAuthToken(admin, "ADMIN");

        // 1. Teacher 1 creates a published exam with SUBJECT_SHARED visibility
        Exam exam1 = Exam.builder()
                .subject(mathSubject)
                .code("EXAM_MATH_SHARED_01")
                .gradeLevel("Lớp 10")
                .title("Đề thi Toán 10 Chuẩn")
                .description("Đề thi chia sẻ toàn bộ môn Toán")
                .instructions("Làm bài 45 phút")
                .durationMinutes(45)
                .totalMarks(new BigDecimal("10.00"))
                .passingMarks(new BigDecimal("5.00"))
                .maxAttempts(1)
                .status(ExamStatus.PUBLISHED)
                .visibility(ExamVisibility.SUBJECT_SHARED)
                .creator(teacher1)
                .build();
        exam1 = examRepository.save(exam1);

        examQuestionRepository.save(ExamQuestion.builder()
                .exam(exam1)
                .question(q1)
                .displayOrder(1)
                .marks(new BigDecimal("1.00"))
                .build());
        examQuestionRepository.save(ExamQuestion.builder()
                .exam(exam1)
                .question(q2)
                .displayOrder(2)
                .marks(new BigDecimal("1.00"))
                .build());

        // 2. Teacher 1 also creates a PRIVATE draft exam
        Exam examPrivate = examRepository.save(Exam.builder()
                .subject(mathSubject)
                .code("EXAM_MATH_PRIVATE_01")
                .gradeLevel("Lớp 10")
                .title("Đề thi riêng tư")
                .durationMinutes(30)
                .totalMarks(new BigDecimal("10.00"))
                .passingMarks(new BigDecimal("5.00"))
                .status(ExamStatus.PUBLISHED)
                .visibility(ExamVisibility.PRIVATE)
                .creator(teacher1)
                .build());

        // 3. Teacher 2 accesses shared library -> sees exam1 but NOT examPrivate
        mockMvc.perform(get("/api/v1/teacher/exams/shared-library")
                        .with(authentication(authTeacher2))
                        .param("subjectId", mathSubject.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[*].id", hasItem(exam1.getId().toString())))
                .andExpect(jsonPath("$[*].id", not(hasItem(examPrivate.getId().toString()))));

        // 4. Teacher 2 clones exam1
        long totalQuestionsBeforeClone = questionRepository.count();
        
        mockMvc.perform(post("/api/v1/teacher/exams/" + exam1.getId() + "/clone")
                        .with(authentication(authTeacher2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("[Bản sao] Đề thi Toán 10 Chuẩn")))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.visibility", is("PRIVATE")))
                .andExpect(jsonPath("$.originExamId", is(exam1.getId().toString())))
                .andExpect(jsonPath("$.originExamTitle", is(exam1.getTitle())))
                .andExpect(jsonPath("$.questionCount", is(2)));

        // Verify Question count in Question Bank has NOT changed (no duplication)
        assertEquals(totalQuestionsBeforeClone, questionRepository.count());

        // 5. Teacher 1 updates exam1 visibility to PUBLIC
        mockMvc.perform(patch("/api/v1/teacher/exams/" + exam1.getId() + "/visibility")
                        .with(authentication(authTeacher1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\": \"PUBLIC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility", is("PUBLIC")));

        // 6. Admin moderates exam1 visibility and lists all exams
        mockMvc.perform(patch("/api/v1/admin/exams/" + exam1.getId() + "/visibility")
                        .with(authentication(authAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\": \"SUBJECT_SHARED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility", is("SUBJECT_SHARED")));

        mockMvc.perform(get("/api/v1/admin/exams")
                        .with(authentication(authAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(exam1.getId().toString())));
    }
}
