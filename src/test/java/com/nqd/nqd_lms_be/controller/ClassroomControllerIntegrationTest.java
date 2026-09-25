package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.classroom.*;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.ClassEnrollmentStatus;
import com.nqd.nqd_lms_be.entity.enums.ClassroomStatus;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.service.classroom.ClassroomService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ClassroomControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    @MockitoBean
    private ClassroomService classroomService;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID classroomId = UUID.randomUUID();

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
                .fullName("Test User")
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(userId);

        AppUserPrincipal principal = new AppUserPrincipal(
                user,
                roles,
                Map.of("sub", userId.toString(), "email", email, "name", "Test User"),
                null,
                null
        );

        return new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google"
        );
    }

    @Test
    @DisplayName("Teacher can create a new classroom")
    void testCreateClassroom() throws Exception {
        OAuth2AuthenticationToken teacherAuth = createAuthToken(teacherId, "teacher@lms.com", Set.of("TEACHER"));

        ClassroomRequest request = ClassroomRequest.builder()
                .name("Lớp 12A1 Chuyên Toán")
                .gradeLevel("Lớp 12")
                .description("Lớp luyện đề nâng cao")
                .build();

        ClassroomResponse response = ClassroomResponse.builder()
                .id(classroomId)
                .name("Lớp 12A1 Chuyên Toán")
                .code("CL12A1")
                .gradeLevel("Lớp 12")
                .teacherId(teacherId)
                .teacherName("Teacher NQD")
                .studentCount(0)
                .status(ClassroomStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(classroomService.createClassroom(any(ClassroomRequest.class), eq(teacherId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/classrooms")
                        .with(authentication(teacherAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(classroomId.toString()))
                .andExpect(jsonPath("$.name").value("Lớp 12A1 Chuyên Toán"))
                .andExpect(jsonPath("$.code").value("CL12A1"));
    }

    @Test
    @DisplayName("Student can request to join classroom by code")
    void testStudentJoinByCode() throws Exception {
        OAuth2AuthenticationToken studentAuth = createAuthToken(studentId, "student@lms.com", Set.of("STUDENT"));

        JoinClassroomRequest request = JoinClassroomRequest.builder()
                .code("CL12A1")
                .message("Em xin vào lớp ạ")
                .build();

        ClassroomStudentResponse response = ClassroomStudentResponse.builder()
                .id(UUID.randomUUID())
                .classroomId(classroomId)
                .studentId(studentId)
                .studentName("Học sinh A")
                .status(ClassEnrollmentStatus.PENDING_APPROVAL)
                .requestMessage("Em xin vào lớp ạ")
                .build();

        when(classroomService.requestToJoinByCode(any(JoinClassroomRequest.class), eq(studentId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/classrooms/join")
                        .with(authentication(studentAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    }

    @Test
    @DisplayName("Teacher can approve student request to join")
    void testTeacherApproveStudent() throws Exception {
        OAuth2AuthenticationToken teacherAuth = createAuthToken(teacherId, "teacher@lms.com", Set.of("TEACHER"));

        ClassroomStudentResponse response = ClassroomStudentResponse.builder()
                .id(UUID.randomUUID())
                .classroomId(classroomId)
                .studentId(studentId)
                .studentName("Học sinh A")
                .status(ClassEnrollmentStatus.ENROLLED)
                .joinedAt(LocalDateTime.now())
                .build();

        when(classroomService.approveStudentRequest(eq(classroomId), eq(studentId), eq(teacherId)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/classrooms/" + classroomId + "/requests/" + studentId + "/approve")
                        .with(authentication(teacherAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENROLLED"));
    }

    @Test
    @DisplayName("Teacher can search users for invitation autocomplete")
    void testSearchUsers() throws Exception {
        OAuth2AuthenticationToken teacherAuth = createAuthToken(teacherId, "teacher@lms.com", Set.of("TEACHER"));

        when(classroomService.searchUsersForInvitation("nam"))
                .thenReturn(List.of(UserSuggestionResponse.builder()
                        .id(studentId)
                        .fullName("Nguyễn Nam")
                        .email("nam@gmail.com")
                        .roles("STUDENT")
                        .build()));

        mockMvc.perform(get("/api/v1/classrooms/search-users?query=nam")
                        .with(authentication(teacherAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("nam@gmail.com"));
    }
}
