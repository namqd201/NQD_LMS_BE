package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AnalyticsIntegrationTest {

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
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User teacherA;
    private User teacherB;
    private User student1;
    private User student2;

    private Course courseA;
    private Chapter chapter1;
    private Lesson lesson1;
    private Lesson lesson2;
    private Exam exam1;

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

        teacherA = userRepository.save(User.builder().email("teacherA_analytics@example.com").fullName("Teacher A").status(UserStatus.ACTIVE).build());
        teacherB = userRepository.save(User.builder().email("teacherB_analytics@example.com").fullName("Teacher B").status(UserStatus.ACTIVE).build());
        student1 = userRepository.save(User.builder().email("student1_analytics@example.com").fullName("Student One").status(UserStatus.ACTIVE).build());
        student2 = userRepository.save(User.builder().email("student2_analytics@example.com").fullName("Student Two").status(UserStatus.ACTIVE).build());

        userRoleRepository.save(UserRole.builder().userId(teacherA.getId()).roleId(teacherRole.getId()).user(teacherA).role(teacherRole).build());
        userRoleRepository.save(UserRole.builder().userId(teacherB.getId()).roleId(teacherRole.getId()).user(teacherB).role(teacherRole).build());
        userRoleRepository.save(UserRole.builder().userId(student1.getId()).roleId(studentRole.getId()).user(student1).role(studentRole).build());
        userRoleRepository.save(UserRole.builder().userId(student2.getId()).roleId(studentRole.getId()).user(student2).role(studentRole).build());

        Subject subject = subjectRepository.save(Subject.builder()
                .name("Data Science")
                .code("DS_" + UUID.randomUUID().toString().substring(0, 5))
                .status(SubjectStatus.ACTIVE)
                .build());

        courseA = courseRepository.save(Course.builder()
                .subject(subject)
                .creator(teacherA)
                .name("Analytics Masterclass")
                .code("ANALYTICS_" + UUID.randomUUID().toString().substring(0, 5))
                .status(CourseStatus.ACTIVE)
                .price(BigDecimal.ZERO)
                .build());

        chapter1 = chapterRepository.save(Chapter.builder()
                .course(courseA)
                .title("Chapter 1: Intro")
                .displayOrder(1)
                .build());

        lesson1 = lessonRepository.save(Lesson.builder()
                .chapter(chapter1)
                .title("Lesson 1: Overview")
                .slug("l1-overview")
                .estimatedMinutes(30)
                .displayOrder(1)
                .status(LessonStatus.PUBLISHED)
                .build());

        lesson2 = lessonRepository.save(Lesson.builder()
                .chapter(chapter1)
                .title("Lesson 2: Deep Dive")
                .slug("l2-deepdive")
                .estimatedMinutes(45)
                .displayOrder(2)
                .status(LessonStatus.PUBLISHED)
                .build());

        exam1 = examRepository.save(Exam.builder()
                .subject(subject)
                .creator(teacherA)
                .course(courseA)
                .title("Midterm Exam")
                .code("EXAM_" + UUID.randomUUID().toString().substring(0, 5))
                .status(ExamStatus.PUBLISHED)
                .durationMinutes(60)
                .totalMarks(BigDecimal.valueOf(10.0))
                .passingMarks(BigDecimal.valueOf(5.0))
                .build());

        // Enroll students
        enrollmentRepository.save(CourseEnrollment.builder()
                .course(courseA)
                .student(student1)
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now().minusDays(3))
                .build());

        enrollmentRepository.save(CourseEnrollment.builder()
                .course(courseA)
                .student(student2)
                .status(EnrollmentStatus.COMPLETED)
                .enrolledAt(LocalDateTime.now().minusDays(5))
                .completedAt(LocalDateTime.now().minusDays(1))
                .build());

        // Seed lesson progress
        lessonProgressRepository.save(LessonProgress.builder()
                .lesson(lesson1)
                .student(student1)
                .status(LessonProgressStatus.COMPLETED)
                .progressPercent(BigDecimal.valueOf(100.0))
                .completedAt(LocalDateTime.now().minusDays(2))
                .lastAccessedAt(LocalDateTime.now().minusDays(2))
                .build());

        lessonProgressRepository.save(LessonProgress.builder()
                .lesson(lesson2)
                .student(student1)
                .status(LessonProgressStatus.IN_PROGRESS)
                .progressPercent(BigDecimal.valueOf(30.0))
                .lastAccessedAt(LocalDateTime.now().minusDays(1))
                .build());

        lessonProgressRepository.save(LessonProgress.builder()
                .lesson(lesson1)
                .student(student2)
                .status(LessonProgressStatus.COMPLETED)
                .progressPercent(BigDecimal.valueOf(100.0))
                .completedAt(LocalDateTime.now().minusDays(3))
                .lastAccessedAt(LocalDateTime.now().minusDays(3))
                .build());

        lessonProgressRepository.save(LessonProgress.builder()
                .lesson(lesson2)
                .student(student2)
                .status(LessonProgressStatus.COMPLETED)
                .progressPercent(BigDecimal.valueOf(100.0))
                .completedAt(LocalDateTime.now().minusDays(1))
                .lastAccessedAt(LocalDateTime.now().minusDays(1))
                .build());

        // Seed exam attempts
        examAttemptRepository.save(ExamAttempt.builder()
                .exam(exam1)
                .student(student1)
                .status(ExamAttemptStatus.SUBMITTED)
                .startedAt(LocalDateTime.now().minusDays(1).minusHours(1))
                .submittedAt(LocalDateTime.now().minusDays(1))
                .totalScore(BigDecimal.valueOf(8.0))
                .percentage(BigDecimal.valueOf(80.0))
                .passed(true)
                .build());

        examAttemptRepository.save(ExamAttempt.builder()
                .exam(exam1)
                .student(student2)
                .status(ExamAttemptStatus.SUBMITTED)
                .startedAt(LocalDateTime.now().minusDays(1).minusHours(1))
                .submittedAt(LocalDateTime.now().minusDays(1))
                .totalScore(BigDecimal.valueOf(9.5))
                .percentage(BigDecimal.valueOf(95.0))
                .passed(true)
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
    @DisplayName("Teacher A can get course analytics with progress, score distribution, and drop-off stats")
    void teacherA_canGetCourseAnalytics() throws Exception {
        OAuth2AuthenticationToken teacherToken = createAuthToken(teacherA, Set.of("TEACHER"));

        mockMvc.perform(get("/api/v1/teacher/courses/" + courseA.getId() + "/analytics")
                        .with(authentication(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(courseA.getId().toString()))
                .andExpect(jsonPath("$.totalEnrolledStudents").value(2))
                .andExpect(jsonPath("$.activeStudents").value(1))
                .andExpect(jsonPath("$.completedStudents").value(1))
                .andExpect(jsonPath("$.droppedStudents").value(0))
                .andExpect(jsonPath("$.passedAttempts").value(2))
                .andExpect(jsonPath("$.failedAttempts").value(0))
                .andExpect(jsonPath("$.passRate").value(100.0))
                .andExpect(jsonPath("$.progressDistribution.range50To75").value(1))
                .andExpect(jsonPath("$.progressDistribution.range75To100").value(1))
                .andExpect(jsonPath("$.scoreDistribution.range7To85").value(1))
                .andExpect(jsonPath("$.scoreDistribution.range85To10").value(1))
                .andExpect(jsonPath("$.dropOffLessons[0].lessonId").value(lesson2.getId().toString()))
                .andExpect(jsonPath("$.dropOffLessons[0].inProgressStudentsCount").value(1))
                .andExpect(jsonPath("$.dropOffLessons[0].completedStudentsCount").value(1))
                .andExpect(jsonPath("$.topStudents.length()").value(2));
    }

    @Test
    @DisplayName("Teacher B cannot view course analytics for course created by Teacher A")
    void teacherB_cannotViewTeacherACourseAnalytics() throws Exception {
        OAuth2AuthenticationToken teacherBToken = createAuthToken(teacherB, Set.of("TEACHER"));

        mockMvc.perform(get("/api/v1/teacher/courses/" + courseA.getId() + "/analytics")
                        .with(authentication(teacherBToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Teacher can inspect detailed progress of a specific student")
    void teacher_canGetStudentProgressDetail() throws Exception {
        OAuth2AuthenticationToken teacherToken = createAuthToken(teacherA, Set.of("TEACHER"));

        mockMvc.perform(get("/api/v1/teacher/students/" + student1.getId() + "/progress")
                        .with(authentication(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(student1.getId().toString()))
                .andExpect(jsonPath("$.studentName").value("Student One"))
                .andExpect(jsonPath("$.courses.length()").value(1))
                .andExpect(jsonPath("$.courses[0].courseId").value(courseA.getId().toString()))
                .andExpect(jsonPath("$.courses[0].completedLessonsCount").value(1))
                .andExpect(jsonPath("$.courses[0].totalLessonsCount").value(2))
                .andExpect(jsonPath("$.courses[0].progressPercent").value(50.0))
                .andExpect(jsonPath("$.courses[0].examAttempts.length()").value(1))
                .andExpect(jsonPath("$.courses[0].examAttempts[0].passed").value(true));
    }

    @Test
    @DisplayName("Student can view their own overall analytics, streak, study hours and activity heatmap")
    void student_canGetSelfOverallAnalytics() throws Exception {
        OAuth2AuthenticationToken studentToken = createAuthToken(student1, Set.of("STUDENT"));

        mockMvc.perform(get("/api/v1/student/progress/analytics")
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedLessons").value(1))
                .andExpect(jsonPath("$.totalStudyHours").value(0.5)) // 30 minutes / 60
                .andExpect(jsonPath("$.averageExamScore").value(80.0))
                .andExpect(jsonPath("$.activityHeatmap").isArray())
                .andExpect(jsonPath("$.recentCourses.length()").value(1))
                .andExpect(jsonPath("$.recentCourses[0].courseId").value(courseA.getId().toString()))
                .andExpect(jsonPath("$.recentCourses[0].progressPercent").value(50.0));
    }
}
