package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.teacher.TeacherResourceRequest;
import com.nqd.nqd_lms_be.dto.student.UpdateLessonProgressRequest;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class Phase5LessonResourceAndProgressTest {

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
    private LessonResourceRepository lessonResourceRepository;

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User teacherA;
    private User teacherB;
    private User student1;
    private User student2;

    private Course activeCourseTeacherA;
    private Course draftCourseTeacherA;
    private Chapter publishedChapter;
    private Lesson publishedLesson;
    private Lesson draftLesson;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role teacherRole = roleRepository.findByName("TEACHER").orElseGet(() -> roleRepository.save(Role.builder().name("TEACHER").description("Teacher").build()));
        Role studentRole = roleRepository.findByName("STUDENT").orElseGet(() -> roleRepository.save(Role.builder().name("STUDENT").description("Student").build()));

        teacherA = userRepository.save(User.builder().email("teacherA_p5@example.com").fullName("Teacher A").status(UserStatus.ACTIVE).build());
        teacherB = userRepository.save(User.builder().email("teacherB_p5@example.com").fullName("Teacher B").status(UserStatus.ACTIVE).build());
        student1 = userRepository.save(User.builder().email("student1_p5@example.com").fullName("Student 1").status(UserStatus.ACTIVE).build());
        student2 = userRepository.save(User.builder().email("student2_p5@example.com").fullName("Student 2").status(UserStatus.ACTIVE).build());

        userRoleRepository.save(UserRole.builder().userId(teacherA.getId()).roleId(teacherRole.getId()).user(teacherA).role(teacherRole).build());
        userRoleRepository.save(UserRole.builder().userId(teacherB.getId()).roleId(teacherRole.getId()).user(teacherB).role(teacherRole).build());
        userRoleRepository.save(UserRole.builder().userId(student1.getId()).roleId(studentRole.getId()).user(student1).role(studentRole).build());
        userRoleRepository.save(UserRole.builder().userId(student2.getId()).roleId(studentRole.getId()).user(student2).role(studentRole).build());

        Subject subject = subjectRepository.save(Subject.builder().name("Computer Science").code("CS_P5_" + UUID.randomUUID().toString().substring(0, 5)).status(SubjectStatus.ACTIVE).build());

        activeCourseTeacherA = courseRepository.save(Course.builder()
                .subject(subject)
                .creator(teacherA)
                .name("Active Java Course")
                .code("JAVA_ACTIVE_" + UUID.randomUUID().toString().substring(0, 5))
                .status(CourseStatus.ACTIVE)
                .build());

        draftCourseTeacherA = courseRepository.save(Course.builder()
                .subject(subject)
                .creator(teacherA)
                .name("Draft Python Course")
                .code("PY_DRAFT_" + UUID.randomUUID().toString().substring(0, 5))
                .status(CourseStatus.DRAFT)
                .build());

        publishedChapter = chapterRepository.save(Chapter.builder()
                .course(activeCourseTeacherA)
                .title("Chapter 1: Basics")
                .displayOrder(1)
                .build());

        publishedLesson = lessonRepository.save(Lesson.builder()
                .chapter(publishedChapter)
                .title("Lesson 1: Intro")
                .slug("lesson-1-intro")
                .displayOrder(1)
                .status(LessonStatus.PUBLISHED)
                .build());

        draftLesson = lessonRepository.save(Lesson.builder()
                .chapter(publishedChapter)
                .title("Lesson 2: Draft Advanced")
                .slug("lesson-2-draft")
                .displayOrder(2)
                .status(LessonStatus.DRAFT)
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

    // ================= 1. TEACHER RESOURCE MANAGEMENT & OWNERSHIP =================

    @Test
    @DisplayName("Teacher A can add and view PDF/VIDEO resources on their lesson")
    void teacherA_canAddAndGetLessonResources() throws Exception {
        OAuth2AuthenticationToken teacherToken = createAuthToken(teacherA, Set.of("TEACHER"));

        TeacherResourceRequest req = TeacherResourceRequest.builder()
                .resourceType(ResourceType.PDF)
                .title("Java CheatSheet PDF")
                .url("https://example.com/java-cheatsheet.pdf")
                .displayOrder(1)
                .build();

        mockMvc.perform(post("/api/v1/teacher/lessons/" + publishedLesson.getId() + "/resources")
                        .with(authentication(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Java CheatSheet PDF"))
                .andExpect(jsonPath("$.resourceType").value("PDF"));

        mockMvc.perform(get("/api/v1/teacher/lessons/" + publishedLesson.getId() + "/resources")
                        .with(authentication(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Java CheatSheet PDF"));
    }

    @Test
    @DisplayName("Teacher B cannot modify resources on Teacher A's lesson (Ownership check throws 403)")
    void teacherB_cannotAddResourceToTeacherALesson() throws Exception {
        OAuth2AuthenticationToken teacherBToken = createAuthToken(teacherB, Set.of("TEACHER"));

        TeacherResourceRequest req = TeacherResourceRequest.builder()
                .resourceType(ResourceType.VIDEO)
                .title("Unauthorized Video")
                .url("https://example.com/video.mp4")
                .build();

        mockMvc.perform(post("/api/v1/teacher/lessons/" + publishedLesson.getId() + "/resources")
                        .with(authentication(teacherBToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ================= 2. STUDENT PUBLISHED CONTENT ACCESS RESTRICTIONS =================

    @Test
    @DisplayName("Student can view resources of published lesson in active course")
    void student_canViewPublishedLessonResources() throws Exception {
        lessonResourceRepository.save(LessonResource.builder()
                .lesson(publishedLesson)
                .resourceType(ResourceType.PDF)
                .title("Public Syllabus PDF")
                .url("https://example.com/syllabus.pdf")
                .displayOrder(1)
                .build());

        OAuth2AuthenticationToken studentToken = createAuthToken(student1, Set.of("STUDENT"));

        mockMvc.perform(get("/api/v1/student/lessons/" + publishedLesson.getId() + "/resources")
                        .with(authentication(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Public Syllabus PDF"));
    }

    @Test
    @DisplayName("Student cannot view resources of draft lesson (Forbidden 403)")
    void student_cannotViewDraftLessonResources() throws Exception {
        lessonResourceRepository.save(LessonResource.builder()
                .lesson(draftLesson)
                .resourceType(ResourceType.PDF)
                .title("Secret Draft PDF")
                .url("https://example.com/draft.pdf")
                .displayOrder(1)
                .build());

        OAuth2AuthenticationToken studentToken = createAuthToken(student1, Set.of("STUDENT"));

        mockMvc.perform(get("/api/v1/student/lessons/" + draftLesson.getId() + "/resources")
                        .with(authentication(studentToken)))
                .andExpect(status().isForbidden());
    }

    // ================= 3. STUDENT PROGRESS TRACKING & CROSS-STUDENT ACCESS =================

    @Test
    @DisplayName("Student 1 can track and complete their own lesson progress")
    void student1_canUpdateOwnProgress() throws Exception {
        OAuth2AuthenticationToken student1Token = createAuthToken(student1, Set.of("STUDENT"));

        UpdateLessonProgressRequest req = UpdateLessonProgressRequest.builder()
                .progressPercent(new BigDecimal("75.00"))
                .completed(false)
                .build();

        mockMvc.perform(put("/api/v1/student/lessons/" + publishedLesson.getId() + "/progress")
                        .with(authentication(student1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.progressPercent").value(75.0));

        // Mark 100% completed
        UpdateLessonProgressRequest completeReq = UpdateLessonProgressRequest.builder()
                .progressPercent(new BigDecimal("100.00"))
                .completed(true)
                .build();

        mockMvc.perform(put("/api/v1/student/lessons/" + publishedLesson.getId() + "/progress")
                        .with(authentication(student1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressPercent").value(100.0));
    }

    @Test
    @DisplayName("Teacher A can view Student progress in their course, but Teacher B cannot")
    void teacher_canViewStudentProgressInManagedCourse() throws Exception {
        // Student 1 makes progress
        lessonProgressRepository.save(LessonProgress.builder()
                .student(student1)
                .lesson(publishedLesson)
                .status(LessonProgressStatus.COMPLETED)
                .progressPercent(new BigDecimal("100.00"))
                .build());

        OAuth2AuthenticationToken teacherAToken = createAuthToken(teacherA, Set.of("TEACHER"));
        mockMvc.perform(get("/api/v1/teacher/courses/" + activeCourseTeacherA.getId() + "/student-progress")
                        .with(authentication(teacherAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentEmail").value("student1_p5@example.com"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        // Teacher B attempts access -> 403 Forbidden
        OAuth2AuthenticationToken teacherBToken = createAuthToken(teacherB, Set.of("TEACHER"));
        mockMvc.perform(get("/api/v1/teacher/courses/" + activeCourseTeacherA.getId() + "/student-progress")
                        .with(authentication(teacherBToken)))
                .andExpect(status().isForbidden());
    }
}
