package com.nqd.nqd_lms_be.controller;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.repository.*;
import org.apache.poi.xwpf.usermodel.*;
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

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class ExamPaperExportIntegrationTest {

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
    private ExamRepository examRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionOptionRepository questionOptionRepository;

    @Autowired
    private ExamQuestionRepository examQuestionRepository;

    private MockMvc mockMvc;
    private User teacherUser;
    private User otherTeacher;
    private Subject mathSubject;
    private Exam testExam;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role teacherRole = roleRepository.findByName("ROLE_TEACHER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_TEACHER").description("Teacher").build()));

        teacherUser = userRepository.save(User.builder()
                .email("teacher_export_" + UUID.randomUUID() + "@nqd.edu.vn")
                .fullName("Cô Nguyễn Thị Giáo Viên")
                .status(UserStatus.ACTIVE)
                .build());

        userRoleRepository.save(UserRole.builder()
                .user(teacherUser)
                .role(teacherRole)
                .userId(teacherUser.getId())
                .roleId(teacherRole.getId())
                .build());

        otherTeacher = userRepository.save(User.builder()
                .email("other_teacher_" + UUID.randomUUID() + "@nqd.edu.vn")
                .fullName("Thầy Trần Văn Khác")
                .status(UserStatus.ACTIVE)
                .build());

        userRoleRepository.save(UserRole.builder()
                .user(otherTeacher)
                .role(teacherRole)
                .userId(otherTeacher.getId())
                .roleId(teacherRole.getId())
                .build());

        mathSubject = subjectRepository.save(Subject.builder()
                .name("Toán")
                .code("MATH_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .description("Môn Toán Tiểu học")
                .build());

        testExam = examRepository.save(Exam.builder()
                .subject(mathSubject)
                .title("ĐỀ KIỂM TRA CUỐI HỌC KÌ I - LỚP 1")
                .code("DE_TOAN_1_01")
                .gradeLevel("Lớp 1")
                .durationMinutes(40)
                .totalMarks(new BigDecimal("10.00"))
                .passingMarks(new BigDecimal("5.00"))
                .status(ExamStatus.PUBLISHED)
                .visibility(ExamVisibility.PRIVATE)
                .creator(teacherUser)
                .build());

        // Question 1: Multiple Choice
        Question mcQuestion = questionRepository.save(Question.builder()
                .subject(mathSubject)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.EASY)
                .content("Số liền sau của số 9 là:")
                .defaultMarks(new BigDecimal("5.00"))
                .creator(teacherUser)
                .build());

        questionOptionRepository.save(QuestionOption.builder()
                .question(mcQuestion)
                .optionKey("A")
                .optionText("Số 8")
                .isCorrect(false)
                .displayOrder(1)
                .build());

        questionOptionRepository.save(QuestionOption.builder()
                .question(mcQuestion)
                .optionKey("B")
                .optionText("Số 10")
                .isCorrect(true)
                .displayOrder(2)
                .build());

        questionOptionRepository.save(QuestionOption.builder()
                .question(mcQuestion)
                .optionKey("C")
                .optionText("Số 7")
                .isCorrect(false)
                .displayOrder(3)
                .build());

        examQuestionRepository.save(ExamQuestion.builder()
                .exam(testExam)
                .question(mcQuestion)
                .displayOrder(1)
                .marks(new BigDecimal("5.00"))
                .build());

        // Question 2: Essay
        Question essayQuestion = questionRepository.save(Question.builder()
                .subject(mathSubject)
                .questionType(QuestionType.ESSAY)
                .difficulty(QuestionDifficulty.MEDIUM)
                .content("Tính: a) 6 + 0 + 4 = .....; b) 10 - 8 + 4 = .....")
                .explanation("a) 6 + 0 + 4 = 10; b) 10 - 8 + 4 = 6")
                .defaultMarks(new BigDecimal("5.00"))
                .creator(teacherUser)
                .build());

        examQuestionRepository.save(ExamQuestion.builder()
                .exam(testExam)
                .question(essayQuestion)
                .displayOrder(2)
                .marks(new BigDecimal("5.00"))
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
    @DisplayName("Teacher should successfully export exam paper to Word (.docx) with custom branding")
    void testExportExamDocx_Success() throws Exception {
        OAuth2AuthenticationToken auth = createAuthToken(teacherUser, "ROLE_TEACHER");

        MvcResult result = mockMvc.perform(get("/api/v1/teacher/exams/{id}/export/docx", testExam.getId())
                        .with(authentication(auth))
                        .param("institutionName", "Trường Tiểu học Đồng Tâm")
                        .param("examTitle", "ĐỀ KIỂM TRA CUỐI HỌC KÌ I - LỚP 1")
                        .param("academicYear", "Năm học: 2014- 2015")
                        .param("subjectName", "Toán")
                        .param("durationMinutes", "40")
                        .param("includeAnswerKey", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0, "Docx file should not be empty");

        // Validate that bytes form a valid XWPFDocument
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes);
             XWPFDocument doc = new XWPFDocument(in)) {
            assertNotNull(doc);
            assertTrue(doc.getParagraphs().size() > 0 || doc.getTables().size() > 0);
        }
    }

    @Test
    @DisplayName("Teacher should successfully export exam paper to PDF (.pdf)")
    void testExportExamPdf_Success() throws Exception {
        OAuth2AuthenticationToken auth = createAuthToken(teacherUser, "ROLE_TEACHER");

        MvcResult result = mockMvc.perform(get("/api/v1/teacher/exams/{id}/export/pdf", testExam.getId())
                        .with(authentication(auth))
                        .param("institutionName", "Trường Tiểu học Đồng Tâm")
                        .param("includeAnswerKey", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0, "PDF file should not be empty");

        // Verify PDF Magic Header %PDF-
        assertEquals('%', (char) bytes[0]);
        assertEquals('P', (char) bytes[1]);
        assertEquals('D', (char) bytes[2]);
        assertEquals('F', (char) bytes[3]);
    }

    @Test
    @DisplayName("Other teacher should be forbidden from exporting private exam of another teacher")
    void testExportExam_ForbiddenForOtherTeacher() throws Exception {
        OAuth2AuthenticationToken auth = createAuthToken(otherTeacher, "ROLE_TEACHER");

        mockMvc.perform(get("/api/v1/teacher/exams/{id}/export/docx", testExam.getId())
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Teacher exporting exam with Short Answer questions should NOT print 'ANS. 98' on paper and should show '98' in Answer Key")
    void testExportExamDocx_WithShortAnswer_HidesAnsAndShowsInKey() throws Exception {
        OAuth2AuthenticationToken auth = createAuthToken(teacherUser, "ROLE_TEACHER");

        Question shortAnsQuestion = questionRepository.save(Question.builder()
                .subject(mathSubject)
                .questionType(QuestionType.SHORT_ANSWER)
                .difficulty(QuestionDifficulty.MEDIUM)
                .content("Số lớn nhất có hai chữ số khác nhau là số nào?")
                .defaultMarks(new BigDecimal("2.00"))
                .creator(teacherUser)
                .build());

        questionOptionRepository.save(QuestionOption.builder()
                .question(shortAnsQuestion)
                .optionKey("ANS")
                .optionText("98")
                .isCorrect(true)
                .displayOrder(1)
                .build());

        examQuestionRepository.save(ExamQuestion.builder()
                .exam(testExam)
                .question(shortAnsQuestion)
                .displayOrder(3)
                .marks(new BigDecimal("2.00"))
                .build());

        MvcResult result = mockMvc.perform(get("/api/v1/teacher/exams/{id}/export/docx", testExam.getId())
                        .with(authentication(auth))
                        .param("includeAnswerKey", "true"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertNotNull(bytes);

        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes);
             XWPFDocument doc = new XWPFDocument(in)) {
            StringBuilder docText = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                docText.append(p.getText()).append("\n");
            }
            assertFalse(docText.toString().contains("ANS. 98"), "Docx must NOT print 'ANS. 98' on question paper");
            assertFalse(docText.toString().contains("ANS."), "Docx must NOT print 'ANS.' on question paper");

            boolean foundAnswerInTable = false;
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        if ("98".equals(cell.getText().trim())) {
                            foundAnswerInTable = true;
                            break;
                        }
                    }
                }
            }
            assertTrue(foundAnswerInTable, "Answer key table must contain the real answer value '98'");
        }
    }
}
