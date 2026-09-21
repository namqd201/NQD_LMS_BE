package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.student.*;
import com.nqd.nqd_lms_be.dto.teacher.TeacherExamResponse;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.ExamAttemptStatus;
import com.nqd.nqd_lms_be.entity.enums.QuestionDifficulty;
import com.nqd.nqd_lms_be.entity.enums.QuestionType;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.service.student.StudentAiTutorService;
import com.nqd.nqd_lms_be.service.student.StudentCourseService;
import com.nqd.nqd_lms_be.service.student.StudentExamService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class StudentAuthorizationTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    @MockitoBean
    private StudentCourseService studentCourseService;

    @MockitoBean
    private StudentExamService studentExamService;

    @MockitoBean
    private StudentAiTutorService studentAiTutorService;

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
    @DisplayName("1. STUDENT can view published courses (returns 200)")
    void student_canViewPublishedCourses() throws Exception {
        UUID studentId = UUID.randomUUID();
        OAuth2AuthenticationToken studentToken = createAuthToken(studentId, "student@example.com", Set.of("STUDENT"));

        StudentCourseResponse course = StudentCourseResponse.builder()
                .id(UUID.randomUUID())
                .name("Calculus 1")
                .code("MATH101")
                .status(CourseStatus.ACTIVE)
                .isEnrolled(false)
                .build();

        when(studentCourseService.getPublishedCourses(eq(studentId))).thenReturn(List.of(course));

        mockMvc.perform(get("/api/v1/student/courses/published")
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Calculus 1"));
    }

    @Test
    @DisplayName("2. STUDENT starting an exam receives sanitized DTO without answer keys or isCorrect")
    void student_startingExam_receivesSanitizedDto() throws Exception {
        UUID studentId = UUID.randomUUID();
        OAuth2AuthenticationToken studentToken = createAuthToken(studentId, "student@example.com", Set.of("STUDENT"));

        UUID examId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        StudentExamTakingResponse takingResponse = StudentExamTakingResponse.builder()
                .examId(examId)
                .attemptId(attemptId)
                .title("Midterm Exam")
                .durationMinutes(45)
                .totalMarks(new BigDecimal("10.00"))
                .attemptNumber(1)
                .startedAt(LocalDateTime.now())
                .questions(List.of(
                        StudentQuestionTakingResponse.builder()
                                .questionId(UUID.randomUUID())
                                .content("What is the derivative of x^2?")
                                .questionType(QuestionType.MULTIPLE_CHOICE)
                                .difficulty(QuestionDifficulty.EASY)
                                .marks(new BigDecimal("5.00"))
                                .displayOrder(1)
                                .options(List.of(
                                        StudentOptionTakingResponse.builder().id(UUID.randomUUID()).optionKey("A").optionText("2x").displayOrder(1).build(),
                                        StudentOptionTakingResponse.builder().id(UUID.randomUUID()).optionKey("B").optionText("x").displayOrder(2).build()
                                ))
                                .build()
                ))
                .build();

        when(studentExamService.startExam(eq(examId), eq(studentId))).thenReturn(takingResponse);

        mockMvc.perform(post("/api/v1/student/exams/" + examId + "/start")
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Midterm Exam"))
                .andExpect(jsonPath("$.questions[0].content").value("What is the derivative of x^2?"))
                .andExpect(jsonPath("$.questions[0].options[0].optionText").value("2x"))
                // Ensure no isCorrect or explanation leaked in JSON response
                .andExpect(jsonPath("$.questions[0].options[0].isCorrect").doesNotExist())
                .andExpect(jsonPath("$.questions[0].explanation").doesNotExist());
    }

    @Test
    @DisplayName("3. STUDENT A attempting to submit STUDENT B's attempt is blocked with 403 (Cross-user access)")
    void studentA_cannotSubmitStudentBAttempt_ownershipCheckThrows403() throws Exception {
        UUID studentAId = UUID.randomUUID();
        OAuth2AuthenticationToken studentAToken = createAuthToken(studentAId, "studentA@example.com", Set.of("STUDENT"));

        UUID attemptBId = UUID.randomUUID();
        SubmitExamAttemptRequest req = SubmitExamAttemptRequest.builder()
                .answers(List.of())
                .build();

        when(studentExamService.submitExam(eq(attemptBId), any(SubmitExamAttemptRequest.class), eq(studentAId)))
                .thenThrow(new ForbiddenOperationException("You cannot submit another student's exam attempt"));

        mockMvc.perform(post("/api/v1/student/exams/attempts/" + attemptBId + "/submit")
                        .with(authentication(studentAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot submit another student's exam attempt"));
    }

    @Test
    @DisplayName("4. STUDENT A attempting to view STUDENT B's attempt result is blocked with 403 (Cross-user access)")
    void studentA_cannotViewStudentBAttemptResult_ownershipCheckThrows403() throws Exception {
        UUID studentAId = UUID.randomUUID();
        OAuth2AuthenticationToken studentAToken = createAuthToken(studentAId, "studentA@example.com", Set.of("STUDENT"));

        UUID attemptBId = UUID.randomUUID();

        when(studentExamService.getAttemptResult(eq(attemptBId), eq(studentAId)))
                .thenThrow(new ForbiddenOperationException("You cannot view another student's exam attempt result"));

        mockMvc.perform(get("/api/v1/student/exams/attempts/" + attemptBId + "/result")
                        .with(authentication(studentAToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot view another student's exam attempt result"));
    }

    @Test
    @DisplayName("5. STUDENT can view own progress (returns 200)")
    void student_canViewOwnProgress() throws Exception {
        UUID studentId = UUID.randomUUID();
        OAuth2AuthenticationToken studentToken = createAuthToken(studentId, "student@example.com", Set.of("STUDENT"));

        StudentProgressResponse progress = StudentProgressResponse.builder()
                .studentId(studentId)
                .totalEnrolledCourses(3)
                .totalExamsTaken(2)
                .passedExams(2)
                .averageScore(88.5)
                .build();

        when(studentExamService.getStudentProgress(eq(studentId))).thenReturn(progress);

        mockMvc.perform(get("/api/v1/student/exams/progress")
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEnrolledCourses").value(3))
                .andExpect(jsonPath("$.averageScore").value(88.5));
    }

    @Test
    @DisplayName("6. STUDENT can use AI Tutor endpoint")
    void student_canUseAiTutor() throws Exception {
        UUID studentId = UUID.randomUUID();
        OAuth2AuthenticationToken studentToken = createAuthToken(studentId, "student@example.com", Set.of("STUDENT"));

        StudentAiTutorRequest req = StudentAiTutorRequest.builder()
                .courseId(UUID.randomUUID())
                .question("Explain Newton's second law")
                .build();

        StudentAiTutorResponse resp = StudentAiTutorResponse.builder()
                .answer("F = ma")
                .context("Physics 101")
                .build();

        when(studentAiTutorService.askAiTutor(any(StudentAiTutorRequest.class), eq(studentId))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/student/ai-tutor/ask")
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("F = ma"));
    }
}
