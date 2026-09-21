package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.student.StudentChapterResponse;
import com.nqd.nqd_lms_be.dto.student.StudentCourseDetailResponse;
import com.nqd.nqd_lms_be.dto.student.StudentLessonDetailResponse;
import com.nqd.nqd_lms_be.dto.student.StudentLessonSummaryResponse;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.LessonStatus;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.service.student.StudentCourseStructureService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class StudentCourseStructureTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    @MockitoBean
    private StudentCourseStructureService studentCourseStructureService;

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
                .fullName("Student User")
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
    @DisplayName("1. Student can view published course structure (returns 200)")
    void student_canViewPublishedCourseStructure() throws Exception {
        UUID studentId = UUID.randomUUID();
        OAuth2AuthenticationToken studentToken = createAuthToken(studentId, "student@example.com", Set.of("STUDENT"));
        UUID courseId = UUID.randomUUID();

        StudentCourseDetailResponse resp = StudentCourseDetailResponse.builder()
                .id(courseId)
                .name("Java Masterclass")
                .status(CourseStatus.ACTIVE)
                .isEnrolled(true)
                .chapters(List.of(
                        StudentChapterResponse.builder()
                                .id(UUID.randomUUID())
                                .title("Chapter 1")
                                .displayOrder(1)
                                .lessons(List.of(
                                        StudentLessonSummaryResponse.builder()
                                                .id(UUID.randomUUID())
                                                .title("Lesson 1.1")
                                                .status(LessonStatus.PUBLISHED)
                                                .displayOrder(1)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(studentCourseStructureService.getPublishedCourseDetail(eq(courseId), eq(studentId)))
                .thenReturn(resp);

        mockMvc.perform(get("/api/v1/student/courses/" + courseId + "/structure")
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java Masterclass"))
                .andExpect(jsonPath("$.chapters[0].title").value("Chapter 1"))
                .andExpect(jsonPath("$.chapters[0].lessons[0].title").value("Lesson 1.1"));
    }

    @Test
    @DisplayName("2. Student querying unpublished course is blocked with 403 (Publishing protection)")
    void student_queryingUnpublishedCourse_blockedWith403() throws Exception {
        UUID studentId = UUID.randomUUID();
        OAuth2AuthenticationToken studentToken = createAuthToken(studentId, "student@example.com", Set.of("STUDENT"));
        UUID unpublishedCourseId = UUID.randomUUID();

        when(studentCourseStructureService.getPublishedCourseDetail(eq(unpublishedCourseId), eq(studentId)))
                .thenThrow(new ForbiddenOperationException("Course is not published or available for students"));

        mockMvc.perform(get("/api/v1/student/courses/" + unpublishedCourseId + "/structure")
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Course is not published or available for students"));
    }

    @Test
    @DisplayName("3. Student can view published lesson content")
    void student_canViewPublishedLesson() throws Exception {
        UUID studentId = UUID.randomUUID();
        OAuth2AuthenticationToken studentToken = createAuthToken(studentId, "student@example.com", Set.of("STUDENT"));
        UUID lessonId = UUID.randomUUID();

        StudentLessonDetailResponse resp = StudentLessonDetailResponse.builder()
                .id(lessonId)
                .title("Introduction to Variables")
                .content("In Java, variables are containers for storing data values.")
                .estimatedMinutes(10)
                .build();

        when(studentCourseStructureService.getPublishedLesson(eq(lessonId), eq(studentId)))
                .thenReturn(resp);

        mockMvc.perform(get("/api/v1/student/lessons/" + lessonId)
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Introduction to Variables"))
                .andExpect(jsonPath("$.content").value("In Java, variables are containers for storing data values."));
    }

    @Test
    @DisplayName("4. Student querying unpublished/draft lesson is blocked with 403 (Publishing protection)")
    void student_queryingDraftLesson_blockedWith403() throws Exception {
        UUID studentId = UUID.randomUUID();
        OAuth2AuthenticationToken studentToken = createAuthToken(studentId, "student@example.com", Set.of("STUDENT"));
        UUID draftLessonId = UUID.randomUUID();

        when(studentCourseStructureService.getPublishedLesson(eq(draftLessonId), eq(studentId)))
                .thenThrow(new ForbiddenOperationException("Lesson is not published or available"));

        mockMvc.perform(get("/api/v1/student/lessons/" + draftLessonId)
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Lesson is not published or available"));
    }

    @Test
    @DisplayName("5. Unauthenticated user accessing student structure endpoints receives 401")
    void unauthenticated_receives401() throws Exception {
        mockMvc.perform(get("/api/v1/student/courses/" + UUID.randomUUID() + "/structure")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
