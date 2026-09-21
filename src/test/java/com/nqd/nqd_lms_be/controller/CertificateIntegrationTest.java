package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.certificate.CertificateResponse;
import com.nqd.nqd_lms_be.dto.student.UpdateLessonProgressRequest;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.service.certificate.CertificateService;
import com.nqd.nqd_lms_be.service.student.StudentProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class CertificateIntegrationTest {

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
    private CertificateRepository certificateRepository;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private StudentProgressService studentProgressService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User teacher;
    private User student1;
    private User student2;

    private Course course;
    private Chapter chapter;
    private Lesson lesson1;
    private Lesson lesson2;
    private CourseEnrollment enrollment1;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role teacherRole = roleRepository.findByName("TEACHER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("TEACHER").description("Teacher").build()));
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("STUDENT").description("Student").build()));

        teacher = userRepository.save(User.builder().email("teacher_cert@example.com").fullName("Teacher Cert").status(UserStatus.ACTIVE).build());
        student1 = userRepository.save(User.builder().email("student1_cert@example.com").fullName("Student One Cert").status(UserStatus.ACTIVE).build());
        student2 = userRepository.save(User.builder().email("student2_cert@example.com").fullName("Student Two Cert").status(UserStatus.ACTIVE).build());

        userRoleRepository.save(UserRole.builder().userId(teacher.getId()).roleId(teacherRole.getId()).user(teacher).role(teacherRole).build());
        userRoleRepository.save(UserRole.builder().userId(student1.getId()).roleId(studentRole.getId()).user(student1).role(studentRole).build());
        userRoleRepository.save(UserRole.builder().userId(student2.getId()).roleId(studentRole.getId()).user(student2).role(studentRole).build());

        Subject math = subjectRepository.save(Subject.builder().name("Toán Học").code("MATH_CERT_" + UUID.randomUUID().toString().substring(0, 5)).build());

        course = courseRepository.save(Course.builder()
                .subject(math)
                .creator(teacher)
                .name("Lập Trình Web Toàn Diện")
                .code("WEB-CERT-" + UUID.randomUUID().toString().substring(0, 5))
                .status(CourseStatus.ACTIVE)
                .price(BigDecimal.ZERO)
                .build());

        chapter = chapterRepository.save(Chapter.builder()
                .course(course)
                .title("Chương 1: Khởi động")
                .displayOrder(1)
                .build());

        lesson1 = lessonRepository.save(Lesson.builder()
                .chapter(chapter)
                .title("Bài 1: Giới thiệu")
                .status(LessonStatus.PUBLISHED)
                .displayOrder(1)
                .estimatedMinutes(30)
                .build());

        lesson2 = lessonRepository.save(Lesson.builder()
                .chapter(chapter)
                .title("Bài 2: Thực hành")
                .status(LessonStatus.PUBLISHED)
                .displayOrder(2)
                .estimatedMinutes(45)
                .build());

        enrollment1 = enrollmentRepository.save(CourseEnrollment.builder()
                .course(course)
                .student(student1)
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .build());
    }

    private OAuth2AuthenticationToken createAuthToken(User user, Set<String> roles) {
        AppUserPrincipal principal = new AppUserPrincipal(
                user,
                roles,
                Map.of("sub", user.getId().toString(), "email", user.getEmail()),
                null,
                null
        );
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    @Test
    @DisplayName("Test 1: Auto issue certificate when student completes 100% course lessons via PUT /api/v1/student/lessons/{id}/progress")
    void testAutoIssueCertificateOnCompletion() throws Exception {
        OAuth2AuthenticationToken token = createAuthToken(student1, Set.of("STUDENT"));

        // Complete lesson 1
        UpdateLessonProgressRequest req1 = UpdateLessonProgressRequest.builder().completed(true).build();
        mockMvc.perform(put("/api/v1/student/lessons/" + lesson1.getId() + "/progress")
                        .with(authentication(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        assertFalse(certificateRepository.existsByStudentIdAndCourseId(student1.getId(), course.getId()),
                "Certificate should not be issued when course is only 50% completed");

        // Complete lesson 2 -> 100%
        UpdateLessonProgressRequest req2 = UpdateLessonProgressRequest.builder().completed(true).build();
        mockMvc.perform(put("/api/v1/student/lessons/" + lesson2.getId() + "/progress")
                        .with(authentication(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk());

        assertTrue(certificateRepository.existsByStudentIdAndCourseId(student1.getId(), course.getId()),
                "Certificate should be automatically issued when all lessons are completed");

        Certificate cert = certificateRepository.findByStudentIdAndCourseId(student1.getId(), course.getId()).orElseThrow();
        assertNotNull(cert.getCertificateCode());
        assertTrue(cert.getCertificateCode().startsWith("CERT-"));
        assertEquals(student1.getId(), cert.getStudent().getId());
        assertEquals(course.getId(), cert.getCourse().getId());
    }

    @Test
    @DisplayName("Test 2: Issuance is idempotent (calling multiple times returns the same certificate)")
    void testCertificateIdempotency() {
        CertificateResponse cert1 = certificateService.issueCertificate(student1.getId(), course.getId());
        assertNotNull(cert1);
        assertNotNull(cert1.getCertificateCode());

        CertificateResponse cert2 = certificateService.issueCertificate(student1.getId(), course.getId());
        assertNotNull(cert2);
        assertEquals(cert1.getId(), cert2.getId());
        assertEquals(cert1.getCertificateCode(), cert2.getCertificateCode());

        long count = certificateRepository.countByCourseId(course.getId());
        assertEquals(1, count, "Only 1 certificate should exist for 1 student and 1 course");
    }

    @Test
    @DisplayName("Test 3: Public verification endpoint is accessible without authentication")
    void testPublicVerificationEndpoint() throws Exception {
        CertificateResponse cert = certificateService.issueCertificate(student1.getId(), course.getId());

        mockMvc.perform(get("/api/v1/public/certificates/verify/" + cert.getCertificateCode())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.certificateCode").value(cert.getCertificateCode()))
                .andExpect(jsonPath("$.studentName").value(student1.getFullName()))
                .andExpect(jsonPath("$.courseName").value(course.getName()))
                .andExpect(jsonPath("$.issuerName").value("NQD Learning Management System"));
    }

    @Test
    @DisplayName("Test 4: PDF generation and download endpoint returns valid PDF stream")
    void testDownloadCertificatePdf() throws Exception {
        CertificateResponse cert = certificateService.issueCertificate(student1.getId(), course.getId());

        MvcResult result = mockMvc.perform(get("/api/v1/certificates/" + cert.getId() + "/download")
                        .with(authentication(createAuthToken(student1, Set.of("STUDENT")))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF size should be at least 500 bytes");

        // Verify PDF Magic Bytes (%PDF-)
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header, "PDF content must start with %PDF- header");
    }

    @Test
    @DisplayName("Test 5: Student cannot download another student's certificate (403 Forbidden)")
    void testStudentCannotDownloadOtherCertificate() throws Exception {
        CertificateResponse cert = certificateService.issueCertificate(student1.getId(), course.getId());

        // Student 2 attempts to download Student 1's certificate
        mockMvc.perform(get("/api/v1/certificates/" + cert.getId() + "/download")
                        .with(authentication(createAuthToken(student2, Set.of("STUDENT")))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test 6: GET /api/v1/certificates/me returns current student's certificates")
    void testGetMyCertificates() throws Exception {
        CertificateResponse cert = certificateService.issueCertificate(student1.getId(), course.getId());

        mockMvc.perform(get("/api/v1/certificates/me")
                        .with(authentication(createAuthToken(student1, Set.of("STUDENT")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].certificateCode").value(cert.getCertificateCode()))
                .andExpect(jsonPath("$[0].courseName").value(course.getName()));
    }
}
