package com.nqd.nqd_lms_be.controller;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.repository.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import java.time.LocalDateTime;
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
class ReportExportIntegrationTest {

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
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    private MockMvc mockMvc;

    private User teacher1;
    private User teacher2;
    private User student;
    private User admin;
    private Course course;
    private Exam exam;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role studentRole = roleRepository.findByName("ROLE_STUDENT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_STUDENT").description("Student").build()));
        Role teacherRole = roleRepository.findByName("ROLE_TEACHER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_TEACHER").description("Teacher").build()));
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").description("Admin").build()));

        teacher1 = userRepository.save(User.builder()
                .email("teacher1_report@nqd.edu.vn")
                .fullName("Thầy Nguyễn Văn Dạy")
                .status(UserStatus.ACTIVE)
                .build());
        userRoleRepository.save(UserRole.builder().user(teacher1).role(teacherRole).userId(teacher1.getId()).roleId(teacherRole.getId()).build());

        teacher2 = userRepository.save(User.builder()
                .email("teacher2_report@nqd.edu.vn")
                .fullName("Cô Trần Thị Giáo")
                .status(UserStatus.ACTIVE)
                .build());
        userRoleRepository.save(UserRole.builder().user(teacher2).role(teacherRole).userId(teacher2.getId()).roleId(teacherRole.getId()).build());

        student = userRepository.save(User.builder()
                .email("student_report@nqd.edu.vn")
                .fullName("Nguyễn Học Sinh")
                .status(UserStatus.ACTIVE)
                .build());
        userRoleRepository.save(UserRole.builder().user(student).role(studentRole).userId(student.getId()).roleId(studentRole.getId()).build());

        admin = userRepository.save(User.builder()
                .email("admin_report@nqd.edu.vn")
                .fullName("Quản Trị Viên Hệ Thống")
                .status(UserStatus.ACTIVE)
                .build());
        userRoleRepository.save(UserRole.builder().user(admin).role(adminRole).userId(admin.getId()).roleId(adminRole.getId()).build());

        Subject mathSubject = subjectRepository.findByCode("MATH_REPORT")
                .orElseGet(() -> subjectRepository.save(Subject.builder()
                        .name("Toán Học Xuất Báo Cáo")
                        .code("MATH_REPORT")
                        .description("Môn Toán báo cáo")
                        .status(SubjectStatus.ACTIVE)
                        .build()));

        course = courseRepository.save(Course.builder()
                .subject(mathSubject)
                .creator(teacher1)
                .name("Toán 12 - Luyện Thi Đại Học Chất Lượng Cao")
                .code("MATH12_REPORT")
                .status(CourseStatus.ACTIVE)
                .build());

        Chapter ch = chapterRepository.save(Chapter.builder()
                .course(course)
                .title("Chương 1: Khảo sát hàm số")
                .displayOrder(1)
                .build());

        lessonRepository.save(Lesson.builder()
                .chapter(ch)
                .title("Bài 1: Tính đơn điệu hàm số")
                .displayOrder(1)
                .status(LessonStatus.PUBLISHED)
                .build());

        CourseEnrollment enrollment = courseEnrollmentRepository.save(CourseEnrollment.builder()
                .course(course)
                .student(student)
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .build());

        exam = examRepository.save(Exam.builder()
                .course(course)
                .subject(mathSubject)
                .creator(teacher1)
                .title("Kiểm tra 1 tiết Đại số 12")
                .code("EXAM_MATH12_01")
                .durationMinutes(45)
                .totalMarks(new BigDecimal("10.00"))
                .passingMarks(new BigDecimal("5.00"))
                .status(ExamStatus.PUBLISHED)
                .build());

        examAttemptRepository.save(ExamAttempt.builder()
                .exam(exam)
                .student(student)
                .attemptNumber(1)
                .totalScore(new BigDecimal("9.50"))
                .percentage(new BigDecimal("95.00"))
                .passed(true)
                .status(ExamAttemptStatus.SUBMITTED)
                .startedAt(LocalDateTime.now().minusMinutes(40))
                .submittedAt(LocalDateTime.now())
                .build());

        certificateRepository.save(Certificate.builder()
                .enrollment(enrollment)
                .course(course)
                .student(student)
                .certificateCode("CERT-NQD-TEST-12345")
                .issuedAt(LocalDateTime.now())
                .isRevoked(false)
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
    @DisplayName("Teacher can export course gradebook Excel (.xlsx) with custom institution branding and dynamic exam scores")
    void testTeacherExportCourseGradebookExcel() throws Exception {
        OAuth2AuthenticationToken teacherAuth = createAuthToken(teacher1, "TEACHER");

        MvcResult result = mockMvc.perform(get("/api/v1/teacher/courses/" + course.getId() + "/report.xlsx")
                        .param("institutionName", "TRƯỜNG THPT CHUYÊN LÊ HỒNG PHONG")
                        .param("reportTitle", "BẢNG ĐIỂM TỔNG KẾT KHÓA LUYỆN THI ĐẠI HỌC")
                        .param("academicYear", "Năm học 2025 - 2026")
                        .param("signerTitle", "HIỆU TRƯỞNG")
                        .with(authentication(teacherAuth)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertNotNull(content);
        assertTrue(content.length > 100);

        // Verify valid XSSFWorkbook
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            assertEquals(2, workbook.getNumberOfSheets());
            assertEquals("Bảng điểm chi tiết", workbook.getSheetAt(0).getSheetName());
            assertEquals("Thống kê lớp học", workbook.getSheetAt(1).getSheetName());

            // Check custom branding text in sheet
            String orgText = workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue();
            assertTrue(orgText.contains("LÊ HỒNG PHONG"));
        }
    }

    @Test
    @DisplayName("Teacher cannot export gradebook of another teacher's course (403 Forbidden)")
    void testTeacherCannotExportOtherTeacherCourse() throws Exception {
        OAuth2AuthenticationToken teacher2Auth = createAuthToken(teacher2, "TEACHER");

        mockMvc.perform(get("/api/v1/teacher/courses/" + course.getId() + "/report.xlsx")
                        .with(authentication(teacher2Auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Student can export personal academic transcript PDF (.pdf) with custom institution header and QR verification")
    void testStudentExportPersonalTranscriptPdf() throws Exception {
        OAuth2AuthenticationToken studentAuth = createAuthToken(student, "STUDENT");

        MvcResult result = mockMvc.perform(get("/api/v1/student/me/transcript.pdf")
                        .param("institutionName", "TRUNG TÂM LUYỆN THI NQD")
                        .param("reportTitle", "HỌC BẠ & BẢNG ĐIỂM HỌC TẬP CÁ NHÂN")
                        .param("academicYear", "Khóa K2025")
                        .param("signerTitle", "GIÁM ĐỐC TRUNG TÂM")
                        .with(authentication(studentAuth)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertNotNull(content);
        assertTrue(content.length > 500);

        // Verify PDF Magic header %PDF-
        String pdfHeader = new String(content, 0, 5);
        assertEquals("%PDF-", pdfHeader);
    }

    @Test
    @DisplayName("Admin can export comprehensive platform overview Excel (.xlsx) with 4 sheets")
    void testAdminExportPlatformOverviewExcel() throws Exception {
        OAuth2AuthenticationToken adminAuth = createAuthToken(admin, "ADMIN");

        MvcResult result = mockMvc.perform(get("/api/v1/admin/reports/platform.xlsx")
                        .param("institutionName", "BAN QUẢN TRỊ NQD LMS")
                        .param("reportTitle", "BÁO CÁO TỔNG QUAN NỀN TẢNG TOÀN DIỆN")
                        .param("academicYear", "Kỳ báo cáo Quý 3/2026")
                        .with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertNotNull(content);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            assertEquals(4, workbook.getNumberOfSheets());
            assertEquals("Tổng quan nền tảng", workbook.getSheetAt(0).getSheetName());
            assertEquals("Khóa học", workbook.getSheetAt(1).getSheetName());
            assertEquals("Đề thi & Kiểm tra", workbook.getSheetAt(2).getSheetName());
            assertEquals("Giao dịch & Doanh thu", workbook.getSheetAt(3).getSheetName());
        }
    }

    @Test
    @DisplayName("Student cannot access Admin platform export endpoint (403 Forbidden)")
    void testStudentCannotExportAdminPlatformReport() throws Exception {
        OAuth2AuthenticationToken studentAuth = createAuthToken(student, "STUDENT");

        mockMvc.perform(get("/api/v1/admin/reports/platform.xlsx")
                        .with(authentication(studentAuth)))
                .andExpect(status().isForbidden());
    }
}
