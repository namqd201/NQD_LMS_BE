package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.teacher.*;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.LessonStatus;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.service.teacher.TeacherCourseStructureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TeacherCourseStructureTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    @MockitoBean
    private TeacherCourseStructureService teacherCourseStructureService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private OAuth2AuthenticationToken createAuthToken(UUID userId, String email, Set<String> roles) {
        User user = User.builder()
                .email(email)
                .fullName("Teacher User")
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(userId);
        AppUserPrincipal principal = new AppUserPrincipal(
                user,
                roles,
                Map.of("sub", "sub-" + userId, "email", email),
                null,
                null
        );
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    @Test
    @DisplayName("1. Teacher can create a chapter in their course")
    void teacher_canCreateChapter() throws Exception {
        UUID teacherId = UUID.randomUUID();
        OAuth2AuthenticationToken teacherToken = createAuthToken(teacherId, "teacher@example.com", Set.of("TEACHER"));
        UUID courseId = UUID.randomUUID();

        TeacherChapterRequest req = TeacherChapterRequest.builder()
                .title("Chapter 1: Intro")
                .description("Basics of Programming")
                .displayOrder(1)
                .build();

        TeacherChapterResponse resp = TeacherChapterResponse.builder()
                .id(UUID.randomUUID())
                .courseId(courseId)
                .title("Chapter 1: Intro")
                .description("Basics of Programming")
                .displayOrder(1)
                .build();

        when(teacherCourseStructureService.createChapter(eq(courseId), any(TeacherChapterRequest.class), eq(teacherId)))
                .thenReturn(resp);

        mockMvc.perform(post("/api/v1/teacher/courses/" + courseId + "/chapters")
                        .with(authentication(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Chapter 1: Intro"));
    }

    @Test
    @DisplayName("2. Teacher can create a lesson in their chapter")
    void teacher_canCreateLesson() throws Exception {
        UUID teacherId = UUID.randomUUID();
        OAuth2AuthenticationToken teacherToken = createAuthToken(teacherId, "teacher@example.com", Set.of("TEACHER"));
        UUID chapterId = UUID.randomUUID();

        TeacherLessonRequest req = TeacherLessonRequest.builder()
                .title("Lesson 1: Hello World")
                .content("Print Hello World in Java")
                .status(LessonStatus.DRAFT)
                .displayOrder(1)
                .build();

        TeacherLessonResponse resp = TeacherLessonResponse.builder()
                .id(UUID.randomUUID())
                .chapterId(chapterId)
                .title("Lesson 1: Hello World")
                .status(LessonStatus.DRAFT)
                .displayOrder(1)
                .build();

        when(teacherCourseStructureService.createLesson(eq(chapterId), any(TeacherLessonRequest.class), eq(teacherId)))
                .thenReturn(resp);

        mockMvc.perform(post("/api/v1/teacher/chapters/" + chapterId + "/lessons")
                        .with(authentication(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Lesson 1: Hello World"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("3. Teacher can publish lesson and publish course")
    void teacher_canPublishLessonAndCourse() throws Exception {
        UUID teacherId = UUID.randomUUID();
        OAuth2AuthenticationToken teacherToken = createAuthToken(teacherId, "teacher@example.com", Set.of("TEACHER"));
        UUID lessonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        TeacherLessonResponse lessonResp = TeacherLessonResponse.builder()
                .id(lessonId)
                .title("Lesson 1")
                .status(LessonStatus.PUBLISHED)
                .build();

        when(teacherCourseStructureService.publishLesson(eq(lessonId), eq(teacherId))).thenReturn(lessonResp);

        mockMvc.perform(put("/api/v1/teacher/lessons/" + lessonId + "/publish")
                        .with(authentication(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        TeacherCourseResponse courseResp = TeacherCourseResponse.builder()
                .id(courseId)
                .name("Java Course")
                .status(CourseStatus.ACTIVE)
                .build();

        when(teacherCourseStructureService.publishCourse(eq(courseId), eq(teacherId))).thenReturn(courseResp);

        mockMvc.perform(put("/api/v1/teacher/courses/" + courseId + "/publish")
                        .with(authentication(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("4. Teacher A cannot modify Teacher B's course structure (Ownership check throws 403)")
    void teacherA_cannotModifyTeacherBCourseStructure_throws403() throws Exception {
        UUID teacherAId = UUID.randomUUID();
        OAuth2AuthenticationToken teacherAToken = createAuthToken(teacherAId, "teacherA@example.com", Set.of("TEACHER"));
        UUID courseBId = UUID.randomUUID();

        TeacherChapterRequest req = TeacherChapterRequest.builder()
                .title("Hacked Chapter")
                .build();

        when(teacherCourseStructureService.createChapter(eq(courseBId), any(TeacherChapterRequest.class), eq(teacherAId)))
                .thenThrow(new ForbiddenOperationException("You do not have permission to manage this course structure"));

        mockMvc.perform(post("/api/v1/teacher/courses/" + courseBId + "/chapters")
                        .with(authentication(teacherAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to manage this course structure"));
    }

    @Test
    @DisplayName("5. Student cannot access Teacher structure modification endpoints (returns 403)")
    void student_cannotAccessTeacherStructureEndpoints_returns403() throws Exception {
        OAuth2AuthenticationToken studentToken = createAuthToken(UUID.randomUUID(), "student@example.com", Set.of("STUDENT"));
        UUID courseId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/teacher/courses/" + courseId + "/publish")
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập chức năng này."));
    }
}
