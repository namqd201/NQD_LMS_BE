package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.ai.dto.GeneratedOptionDraft;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.teacher.*;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class TeacherAiQuestionAndExamGenerationTest {

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
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionOptionRepository questionOptionRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private AiGenerationJobRepository aiGenerationJobRepository;

    @Autowired
    private AiGeneratedQuestionRepository aiGeneratedQuestionRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private User teacher1;
    private User teacher2;
    private User student1;
    private Subject mathSubject;
    private Course teacher1Course;

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
                .email("teacher_ai_1@lms.com")
                .fullName("Teacher AI One")
                .status(UserStatus.ACTIVE)
                .build());
        userRoleRepository.save(UserRole.builder().userId(teacher1.getId()).roleId(teacherRole.getId()).build());

        teacher2 = userRepository.save(User.builder()
                .email("teacher_ai_2@lms.com")
                .fullName("Teacher AI Two")
                .status(UserStatus.ACTIVE)
                .build());
        userRoleRepository.save(UserRole.builder().userId(teacher2.getId()).roleId(teacherRole.getId()).build());

        MembershipPlan teacherPro = membershipPlanRepository.findByPlanCodeAndIsDeletedFalse("TEACHER_PRO_MONTHLY")
                .orElseGet(() -> membershipPlanRepository.save(MembershipPlan.builder()
                        .planCode("TEACHER_PRO_MONTHLY")
                        .name("Teacher Pro")
                        .userType(PlanUserType.TEACHER)
                        .price(BigDecimal.valueOf(199000))
                        .currency("VND")
                        .billingCycle(BillingCycle.MONTHLY)
                        .features("[\"AI_EXAM_GENERATION\", \"ADVANCED_ANALYTICS\", \"PDF_DOWNLOAD\", \"VIDEO_HIGH_QUALITY\"]")
                        .active(true)
                        .build()));

        subscriptionRepository.save(Subscription.builder()
                .user(teacher1)
                .membershipPlan(teacherPro)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(java.time.LocalDateTime.now().minusDays(1))
                .endDate(java.time.LocalDateTime.now().plusMonths(1))
                .autoRenew(false)
                .build());

        subscriptionRepository.save(Subscription.builder()
                .user(teacher2)
                .membershipPlan(teacherPro)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(java.time.LocalDateTime.now().minusDays(1))
                .endDate(java.time.LocalDateTime.now().plusMonths(1))
                .autoRenew(false)
                .build());

        student1 = userRepository.save(User.builder()
                .email("student_ai_1@lms.com")
                .fullName("Student AI One")
                .status(UserStatus.ACTIVE)
                .build());
        userRoleRepository.save(UserRole.builder().userId(student1.getId()).roleId(studentRole.getId()).build());

        mathSubject = subjectRepository.save(Subject.builder()
                .code("MATH-AI")
                .name("Toán Học AI")
                .status(SubjectStatus.ACTIVE)
                .build());

        teacher1Course = courseRepository.save(Course.builder()
                .creator(teacher1)
                .subject(mathSubject)
                .name("Toán Lớp 1 Nâng Cao")
                .code("MATH-101-AI")
                .gradeLevel("Lớp 1")
                .status(CourseStatus.ACTIVE)
                .build());
    }

    private OAuth2AuthenticationToken createToken(User user, String role) {
        AppUserPrincipal principal = new AppUserPrincipal(
                user,
                Set.of(role),
                Map.of("sub", "sub-" + user.getId(), "email", user.getEmail(), "name", user.getFullName()),
                null,
                null
        );
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    @Test
    @DisplayName("Teacher can request AI question generation and receive validated structured questions")
    void testTeacherGenerateQuestionsSuccess() throws Exception {
        TeacherAiGenerateQuestionsRequest req = TeacherAiGenerateQuestionsRequest.builder()
                .subjectId(mathSubject.getId())
                .courseId(teacher1Course.getId())
                .gradeLevel("Lớp 1")
                .topic("Phép cộng trong phạm vi 10")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.EASY)
                .numberOfQuestions(3)
                .marksPerQuestion(new BigDecimal("1.50"))
                .additionalInstructions("Tập trung vào phép cộng có hình minh họa và lời giải dễ hiểu")
                .build();

        mockMvc.perform(post("/api/v1/teacher/ai/questions/generate")
                        .with(authentication(createToken(teacher1, "ROLE_TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.job.id").isNotEmpty())
                .andExpect(jsonPath("$.job.jobType").value("QUESTION_GENERATION"))
                .andExpect(jsonPath("$.job.status").value("COMPLETED"))
                .andExpect(jsonPath("$.job.totalGenerated").value(3))
                .andExpect(jsonPath("$.questions", hasSize(3)))
                .andExpect(jsonPath("$.questions[0].validationStatus").value("VALID"))
                .andExpect(jsonPath("$.questions[0].reviewStatus").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.questions[0].options", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("Teacher can approve a generated question into Question Bank")
    void testTeacherApproveGeneratedQuestion() throws Exception {
        // 1. Generate questions
        TeacherAiGenerateQuestionsRequest req = TeacherAiGenerateQuestionsRequest.builder()
                .subjectId(mathSubject.getId())
                .gradeLevel("Lớp 1")
                .topic("Phép trừ trong phạm vi 10")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.EASY)
                .numberOfQuestions(2)
                .build();

        String responseStr = mockMvc.perform(post("/api/v1/teacher/ai/questions/generate")
                        .with(authentication(createToken(teacher1, "ROLE_TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        TeacherAiJobDetailResponse jobDetail = objectMapper.readValue(responseStr, TeacherAiJobDetailResponse.class);
        var qToApprove = jobDetail.getQuestions().get(0);

        // 2. Approve single question
        mockMvc.perform(post("/api/v1/teacher/ai/jobs/" + jobDetail.getJob().getId() + "/questions/" + qToApprove.getId() + "/approve")
                        .with(authentication(createToken(teacher1, "ROLE_TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("APPROVED"))
                .andExpect(jsonPath("$.approvedQuestionId").isNotEmpty());

        // 3. Verify real Question entity exists in Question Bank with AI_GENERATED source
        var realQuestions = questionRepository.findByCreatorId(teacher1.getId());
        assertFalse(realQuestions.isEmpty());
        Question savedRealQ = realQuestions.get(0);
        assertEquals(QuestionSource.AI_GENERATED, savedRealQ.getSource());
        assertEquals(QuestionStatus.APPROVED, savedRealQ.getStatus());
        assertEquals(mathSubject.getId(), savedRealQ.getSubject().getId());
    }

    @Test
    @DisplayName("Teacher can bulk approve all valid questions from AI generation job")
    void testTeacherBulkApproveAllValidQuestions() throws Exception {
        TeacherAiGenerateQuestionsRequest req = TeacherAiGenerateQuestionsRequest.builder()
                .subjectId(mathSubject.getId())
                .gradeLevel("Lớp 1")
                .topic("Hình học trực quan")
                .numberOfQuestions(4)
                .build();

        String responseStr = mockMvc.perform(post("/api/v1/teacher/ai/questions/generate")
                        .with(authentication(createToken(teacher1, "ROLE_TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        TeacherAiJobDetailResponse jobDetail = objectMapper.readValue(responseStr, TeacherAiJobDetailResponse.class);

        // Bulk approve
        mockMvc.perform(post("/api/v1/teacher/ai/jobs/" + jobDetail.getJob().getId() + "/approve-all")
                        .with(authentication(createToken(teacher1, "ROLE_TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobDetail.getJob().getId().toString()))
                .andExpect(jsonPath("$.approvedQuestionsCount").value(4));

        var realQuestions = questionRepository.findByCreatorId(teacher1.getId());
        assertEquals(4, realQuestions.size());
    }

    @Test
    @DisplayName("Teacher can generate Exam from blueprint and create draft Exam directly")
    void testTeacherGenerateExamAndCreateDraftExam() throws Exception {
        TeacherAiGenerateExamRequest req = TeacherAiGenerateExamRequest.builder()
                .subjectId(mathSubject.getId())
                .courseId(teacher1Course.getId())
                .title("Kiểm Tra Giữa Kỳ Toán Lớp 1 AI")
                .gradeLevel("Lớp 1")
                .durationMinutes(45)
                .passingMarks(new BigDecimal("5.00"))
                .blueprintItems(List.of(
                        ExamBlueprintItemRequest.builder()
                                .topic("Phép cộng")
                                .questionType(QuestionType.MULTIPLE_CHOICE)
                                .difficulty(QuestionDifficulty.EASY)
                                .count(2)
                                .marksPerQuestion(new BigDecimal("2.00"))
                                .build(),
                        ExamBlueprintItemRequest.builder()
                                .topic("Phép trừ")
                                .questionType(QuestionType.TRUE_FALSE)
                                .difficulty(QuestionDifficulty.MEDIUM)
                                .count(2)
                                .marksPerQuestion(new BigDecimal("3.00"))
                                .build()
                ))
                .build();

        String responseStr = mockMvc.perform(post("/api/v1/teacher/ai/exams/generate")
                        .with(authentication(createToken(teacher1, "ROLE_TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.job.jobType").value("EXAM_GENERATION"))
                .andExpect(jsonPath("$.job.totalGenerated").value(4))
                .andReturn().getResponse().getContentAsString();

        TeacherAiJobDetailResponse jobDetail = objectMapper.readValue(responseStr, TeacherAiJobDetailResponse.class);

        // Create exam from job
        mockMvc.perform(post("/api/v1/teacher/ai/jobs/" + jobDetail.getJob().getId() + "/create-exam")
                        .with(authentication(createToken(teacher1, "ROLE_TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdExamId").isNotEmpty())
                .andExpect(jsonPath("$.approvedQuestionsCount").value(4));

        var exams = examRepository.findByCreatorId(teacher1.getId());
        assertFalse(exams.isEmpty());
        Exam createdExam = exams.get(0);
        assertEquals("Kiểm Tra Giữa Kỳ Toán Lớp 1 AI", createdExam.getTitle());
        assertEquals(ExamStatus.DRAFT, createdExam.getStatus());
        assertEquals(new BigDecimal("10.00"), createdExam.getTotalMarks());
    }

    @Test
    @DisplayName("Teacher can edit generated question before approving")
    void testTeacherEditGeneratedQuestion() throws Exception {
        TeacherAiGenerateQuestionsRequest req = TeacherAiGenerateQuestionsRequest.builder()
                .subjectId(mathSubject.getId())
                .topic("Đo độ dài")
                .numberOfQuestions(1)
                .build();

        String responseStr = mockMvc.perform(post("/api/v1/teacher/ai/questions/generate")
                        .with(authentication(createToken(teacher1, "ROLE_TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();

        TeacherAiJobDetailResponse jobDetail = objectMapper.readValue(responseStr, TeacherAiJobDetailResponse.class);
        var q = jobDetail.getQuestions().get(0);

        TeacherAiUpdateGeneratedQuestionRequest updateReq = TeacherAiUpdateGeneratedQuestionRequest.builder()
                .content("Nội dung câu hỏi đã được giáo viên chỉnh sửa chuẩn xác hơn: 1dm bằng bao nhiêu cm?")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.EASY)
                .marks(new BigDecimal("2.00"))
                .explanation("1dm = 10cm theo bảng đo độ dài")
                .tags("Toán 1, Đo lường")
                .options(List.of(
                        GeneratedOptionDraft.builder().optionKey("A").optionText("10 cm").isCorrect(true).displayOrder(1).build(),
                        GeneratedOptionDraft.builder().optionKey("B").optionText("100 cm").isCorrect(false).displayOrder(2).build(),
                        GeneratedOptionDraft.builder().optionKey("C").optionText("1 cm").isCorrect(false).displayOrder(3).build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/teacher/ai/jobs/" + jobDetail.getJob().getId() + "/questions/" + q.getId())
                        .with(authentication(createToken(teacher1, "ROLE_TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(containsString("1dm bằng bao nhiêu cm?")))
                .andExpect(jsonPath("$.marks").value(2.0))
                .andExpect(jsonPath("$.validationStatus").value("VALID"));
    }

    @Test
    @DisplayName("Student is FORBIDDEN from accessing AI generation APIs")
    void testStudentForbiddenFromAiGenerationApis() throws Exception {
        TeacherAiGenerateQuestionsRequest req = TeacherAiGenerateQuestionsRequest.builder()
                .subjectId(mathSubject.getId())
                .topic("Hack attempt")
                .numberOfQuestions(1)
                .build();

        // Student tries to generate questions -> 403
        mockMvc.perform(post("/api/v1/teacher/ai/questions/generate")
                        .with(authentication(createToken(student1, "ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // Student tries to list AI jobs -> 403
        mockMvc.perform(get("/api/v1/teacher/ai/jobs")
                        .with(authentication(createToken(student1, "ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Teacher cannot access another teacher's AI generation job")
    void testTeacherOwnershipEnforcementOnAiJobs() throws Exception {
        TeacherAiGenerateQuestionsRequest req = TeacherAiGenerateQuestionsRequest.builder()
                .subjectId(mathSubject.getId())
                .topic("Chủ đề riêng của Teacher 1")
                .numberOfQuestions(1)
                .build();

        String responseStr = mockMvc.perform(post("/api/v1/teacher/ai/questions/generate")
                        .with(authentication(createToken(teacher1, "ROLE_TEACHER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();

        TeacherAiJobDetailResponse jobDetail = objectMapper.readValue(responseStr, TeacherAiJobDetailResponse.class);

        // Teacher 2 tries to access Teacher 1's job -> 403 Forbidden
        mockMvc.perform(get("/api/v1/teacher/ai/jobs/" + jobDetail.getJob().getId())
                        .with(authentication(createToken(teacher2, "ROLE_TEACHER"))))
                .andExpect(status().isForbidden());
    }
}
