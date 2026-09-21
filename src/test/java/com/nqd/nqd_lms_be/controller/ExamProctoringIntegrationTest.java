package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.student.ExamAttemptEventRequest;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class ExamProctoringIntegrationTest {

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

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private ExamAttemptEventRepository examAttemptEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private User student;
    private User teacher;
    private Subject mathSubject;
    private Question q1;
    private Exam proctoredExam;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role studentRole = roleRepository.findByName("ROLE_STUDENT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_STUDENT").description("Student").build()));
        Role teacherRole = roleRepository.findByName("ROLE_TEACHER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_TEACHER").description("Teacher").build()));

        student = userRepository.findByEmail("student_proctor@nqd.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("student_proctor@nqd.edu.vn")
                        .fullName("Student Proctoring Test")
                        .status(UserStatus.ACTIVE)
                        .build()));
        if (userRoleRepository.findByUserId(student.getId()).isEmpty()) {
            userRoleRepository.save(UserRole.builder().user(student).role(studentRole).userId(student.getId()).roleId(studentRole.getId()).build());
        }

        teacher = userRepository.findByEmail("teacher_proctor@nqd.edu.vn")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("teacher_proctor@nqd.edu.vn")
                        .fullName("Teacher Proctoring Test")
                        .status(UserStatus.ACTIVE)
                        .build()));
        if (userRoleRepository.findByUserId(teacher.getId()).isEmpty()) {
            userRoleRepository.save(UserRole.builder().user(teacher).role(teacherRole).userId(teacher.getId()).roleId(teacherRole.getId()).build());
        }

        mathSubject = subjectRepository.findByCode("MATH_PROCTOR")
                .orElseGet(() -> subjectRepository.save(Subject.builder()
                        .name("Toán Học Proctor")
                        .code("MATH_PROCTOR")
                        .description("Môn Toán thi có giám sát")
                        .status(SubjectStatus.ACTIVE)
                        .build()));

        q1 = questionRepository.save(Question.builder()
                .subject(mathSubject)
                .gradeLevel("Lớp 12")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.EASY)
                .content("Căn bậc hai của 16 là bao nhiêu?")
                .defaultMarks(new BigDecimal("10.00"))
                .status(QuestionStatus.APPROVED)
                .creator(teacher)
                .build());

        proctoredExam = examRepository.save(Exam.builder()
                .subject(mathSubject)
                .code("EXAM_PROCTOR_01")
                .gradeLevel("Lớp 12")
                .title("Đề thi Toán 12 - Giám sát Chống gian lận")
                .durationMinutes(45)
                .totalMarks(new BigDecimal("10.00"))
                .passingMarks(new BigDecimal("5.00"))
                .maxAttempts(2)
                .status(ExamStatus.PUBLISHED)
                .visibility(ExamVisibility.PUBLIC)
                .enableProctoring(true)
                .maxViolationCount(3)
                .creator(teacher)
                .build());

        examQuestionRepository.save(ExamQuestion.builder()
                .exam(proctoredExam)
                .question(q1)
                .displayOrder(1)
                .marks(new BigDecimal("10.00"))
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
    @DisplayName("Should track exam proctoring events, increment violation count, auto-flag and submit when threshold reached")
    void testExamProctoringWorkflow() throws Exception {
        OAuth2AuthenticationToken authStudent = createAuthToken(student, "STUDENT");
        OAuth2AuthenticationToken authTeacher = createAuthToken(teacher, "TEACHER");

        // 1. Student starts exam
        String startResponse = mockMvc.perform(post("/api/v1/student/exams/" + proctoredExam.getId() + "/start")
                        .with(authentication(authStudent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enableProctoring", is(true)))
                .andExpect(jsonPath("$.maxViolationCount", is(3)))
                .andExpect(jsonPath("$.violationCount", is(0)))
                .andExpect(jsonPath("$.isFlagged", is(false)))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> map = objectMapper.readValue(startResponse, Map.class);
        String attemptIdStr = (String) map.get("attemptId");

        // 2. Student sends 2 violation events (TAB_BLUR & FULLSCREEN_EXIT)
        List<ExamAttemptEventRequest> eventBatch1 = List.of(
                ExamAttemptEventRequest.builder()
                        .eventType(ExamAttemptEventType.TAB_BLUR)
                        .occurredAt(LocalDateTime.now())
                        .metadata("Rời khỏi tab thi sang ứng dụng khác")
                        .isViolation(true)
                        .build(),
                ExamAttemptEventRequest.builder()
                        .eventType(ExamAttemptEventType.FULLSCREEN_EXIT)
                        .occurredAt(LocalDateTime.now())
                        .metadata("Thoát chế độ toàn màn hình")
                        .isViolation(true)
                        .build()
        );

        mockMvc.perform(post("/api/v1/student/exams/attempts/" + attemptIdStr + "/events")
                        .with(authentication(authStudent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventBatch1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violationCount", is(2)))
                .andExpect(jsonPath("$.maxViolationCount", is(3)))
                .andExpect(jsonPath("$.isFlagged", is(false)))
                .andExpect(jsonPath("$.isAutoSubmitted", is(false)));

        // 3. Student sends 1 more violation event (COPY_ATTEMPT) -> reaches threshold 3
        List<ExamAttemptEventRequest> eventBatch2 = List.of(
                ExamAttemptEventRequest.builder()
                        .eventType(ExamAttemptEventType.COPY_ATTEMPT)
                        .occurredAt(LocalDateTime.now())
                        .metadata("Cố ý sao chép nội dung câu hỏi")
                        .isViolation(true)
                        .build()
        );

        mockMvc.perform(post("/api/v1/student/exams/attempts/" + attemptIdStr + "/events")
                        .with(authentication(authStudent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventBatch2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violationCount", is(3)))
                .andExpect(jsonPath("$.maxViolationCount", is(3)))
                .andExpect(jsonPath("$.isFlagged", is(true)))
                .andExpect(jsonPath("$.isAutoSubmitted", is(true)));

        // 4. Verify in DB that attempt is marked SUBMITTED and isFlagged = true
        ExamAttempt attemptInDb = examAttemptRepository.findById(java.util.UUID.fromString(attemptIdStr)).orElseThrow();
        assertEquals(ExamAttemptStatus.SUBMITTED, attemptInDb.getStatus());
        assertTrue(attemptInDb.getIsFlagged());
        assertEquals(3, attemptInDb.getViolationCount());
        assertNotNull(attemptInDb.getFlagReason());

        // 5. Teacher checks attempt details & proctoring report
        mockMvc.perform(get("/api/v1/teacher/exams/attempts/" + attemptIdStr + "/detail")
                        .with(authentication(authTeacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId", is(attemptIdStr)))
                .andExpect(jsonPath("$.violationCount", is(3)))
                .andExpect(jsonPath("$.isFlagged", is(true)))
                .andExpect(jsonPath("$.flagReason", containsString("Vượt quá số lần cảnh báo gian lận")))
                .andExpect(jsonPath("$.events", hasSize(3)))
                .andExpect(jsonPath("$.events[0].eventType", is("TAB_BLUR")))
                .andExpect(jsonPath("$.events[1].eventType", is("FULLSCREEN_EXIT")))
                .andExpect(jsonPath("$.events[2].eventType", is("COPY_ATTEMPT")));
    }
}
