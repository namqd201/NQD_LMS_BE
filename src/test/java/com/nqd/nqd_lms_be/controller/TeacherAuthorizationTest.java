package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.teacher.*;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.ExamStatus;
import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.service.teacher.TeacherCourseService;
import com.nqd.nqd_lms_be.service.teacher.TeacherExamService;
import com.nqd.nqd_lms_be.service.teacher.TeacherQuestionService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TeacherAuthorizationTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    @MockitoBean
    private TeacherCourseService teacherCourseService;

    @MockitoBean
    private TeacherQuestionService teacherQuestionService;

    @MockitoBean
    private TeacherExamService teacherExamService;

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
    @DisplayName("1. TEACHER role can access GET /api/v1/teacher/courses (returns 200)")
    void teacher_canAccessTeacherCourses() throws Exception {
        UUID teacherId = UUID.randomUUID();
        OAuth2AuthenticationToken teacherToken = createAuthToken(teacherId, "teacher@example.com", Set.of("TEACHER"));

        TeacherCourseResponse response = TeacherCourseResponse.builder()
                .id(UUID.randomUUID())
                .name("Java Masterclass")
                .code("JAVA101")
                .status(CourseStatus.ACTIVE)
                .creatorId(teacherId)
                .build();

        when(teacherCourseService.getTeacherCourses(eq(teacherId))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/teacher/courses")
                        .with(authentication(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java Masterclass"));
    }

    @Test
    @DisplayName("2. STUDENT attempting to access /api/v1/teacher/courses is denied (returns 403)")
    void student_cannotAccessTeacherCourses_returns403() throws Exception {
        OAuth2AuthenticationToken studentToken = createAuthToken(UUID.randomUUID(), "student@example.com", Set.of("STUDENT"));

        mockMvc.perform(get("/api/v1/teacher/courses")
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập chức năng này."));
    }

    @Test
    @DisplayName("3. STUDENT attempting to delete from Question Bank (/api/v1/teacher/questions/{id}) is denied (returns 403)")
    void student_cannotAccessQuestionBank_returns403() throws Exception {
        OAuth2AuthenticationToken studentToken = createAuthToken(UUID.randomUUID(), "student@example.com", Set.of("STUDENT"));

        mockMvc.perform(delete("/api/v1/teacher/questions/" + UUID.randomUUID())
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. STUDENT attempting to create exam (/api/v1/teacher/exams) is denied (returns 403)")
    void student_cannotCreateExam_returns403() throws Exception {
        OAuth2AuthenticationToken studentToken = createAuthToken(UUID.randomUUID(), "student@example.com", Set.of("STUDENT"));
        TeacherExamRequest req = TeacherExamRequest.builder()
                .subjectId(UUID.randomUUID())
                .title("Final Exam")
                .durationMinutes(60)
                .build();

        mockMvc.perform(post("/api/v1/teacher/exams")
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. STUDENT attempting to access Teacher Analytics is denied (returns 403)")
    void student_cannotAccessTeacherAnalytics_returns403() throws Exception {
        OAuth2AuthenticationToken studentToken = createAuthToken(UUID.randomUUID(), "student@example.com", Set.of("STUDENT"));
        UUID courseId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/teacher/courses/" + courseId + "/analytics")
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. TEACHER A attempting to update TEACHER B's course fails ownership check (returns 403)")
    void teacherA_cannotModifyTeacherBCourse_ownershipCheckThrows403() throws Exception {
        UUID teacherAId = UUID.randomUUID();
        OAuth2AuthenticationToken teacherAToken = createAuthToken(teacherAId, "teacherA@example.com", Set.of("TEACHER"));

        UUID courseBId = UUID.randomUUID();
        TeacherCourseRequest req = TeacherCourseRequest.builder()
                .subjectId(UUID.randomUUID())
                .name("Updated Title")
                .code("CODE2")
                .build();

        when(teacherCourseService.updateCourse(eq(courseBId), any(TeacherCourseRequest.class), eq(teacherAId)))
                .thenThrow(new ForbiddenOperationException("You do not have permission to manage this course"));

        mockMvc.perform(put("/api/v1/teacher/courses/" + courseBId)
                        .with(authentication(teacherAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to manage this course"));
    }

    @Test
    @DisplayName("7. TEACHER A attempting to delete TEACHER B's question fails ownership check (returns 403)")
    void teacherA_cannotDeleteTeacherBQuestion_ownershipCheckThrows403() throws Exception {
        UUID teacherAId = UUID.randomUUID();
        OAuth2AuthenticationToken teacherAToken = createAuthToken(teacherAId, "teacherA@example.com", Set.of("TEACHER"));
        UUID questionBId = UUID.randomUUID();

        doThrow(new ForbiddenOperationException("You do not have permission to modify this question"))
                .when(teacherQuestionService).deleteQuestion(eq(questionBId), eq(teacherAId));

        mockMvc.perform(delete("/api/v1/teacher/questions/" + questionBId)
                        .with(authentication(teacherAToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to modify this question"));
    }

    @Test
    @DisplayName("8. TEACHER can create questions with answers and explanations (returns 201)")
    void teacher_canCreateQuestionWithExplanation() throws Exception {
        UUID teacherId = UUID.randomUUID();
        OAuth2AuthenticationToken teacherToken = createAuthToken(teacherId, "teacher@example.com", Set.of("TEACHER"));

        TeacherQuestionRequest req = TeacherQuestionRequest.builder()
                .subjectId(UUID.randomUUID())
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.MEDIUM)
                .content("What is polymorphism?")
                .explanation("Polymorphism allows objects of different classes to be treated as objects of a common super class.")
                .defaultMarks(new BigDecimal("2.00"))
                .options(List.of(
                        TeacherQuestionOptionDto.builder().optionKey("A").optionText("Option 1").isCorrect(true).displayOrder(1).build(),
                        TeacherQuestionOptionDto.builder().optionKey("B").optionText("Option 2").isCorrect(false).displayOrder(2).build()
                ))
                .build();

        TeacherQuestionResponse resp = TeacherQuestionResponse.builder()
                .id(UUID.randomUUID())
                .content(req.getContent())
                .explanation(req.getExplanation())
                .questionType(req.getQuestionType())
                .difficulty(req.getDifficulty())
                .creatorId(teacherId)
                .build();

        when(teacherQuestionService.createQuestion(any(TeacherQuestionRequest.class), eq(teacherId))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/teacher/questions")
                        .with(authentication(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("What is polymorphism?"))
                .andExpect(jsonPath("$.explanation").isNotEmpty());
    }
}
