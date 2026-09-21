package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.user.UpdateProfileRequest;
import com.nqd.nqd_lms_be.dto.user.UserProfileResponse;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.service.teacher.TeacherProfileService;
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

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TeacherProfileTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    @MockitoBean
    private TeacherProfileService teacherProfileService;

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
    @DisplayName("1. Teacher can view own profile via GET /api/v1/teacher/profile")
    void teacher_canViewOwnProfile() throws Exception {
        UUID teacherId = UUID.randomUUID();
        OAuth2AuthenticationToken teacherToken = createAuthToken(teacherId, "teacher@test.com", Set.of("TEACHER"));

        UserProfileResponse response = UserProfileResponse.builder()
                .id(teacherId)
                .email("teacher@test.com")
                .fullName("Teacher User")
                .phoneNumber("0912345678")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("TEACHER"))
                .build();

        when(teacherProfileService.getMyProfile(eq(teacherId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/teacher/profile")
                        .with(authentication(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("teacher@test.com"))
                .andExpect(jsonPath("$.fullName").value("Teacher User"))
                .andExpect(jsonPath("$.roles[0]").value("TEACHER"));
    }

    @Test
    @DisplayName("2. Teacher can update allowed profile fields (fullName, phoneNumber, avatarUrl)")
    void teacher_canUpdateAllowedProfileFields() throws Exception {
        UUID teacherId = UUID.randomUUID();
        OAuth2AuthenticationToken teacherToken = createAuthToken(teacherId, "teacher@test.com", Set.of("TEACHER"));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Dr. Teacher Updated")
                .phoneNumber("0988888888")
                .avatarUrl("https://example.com/teacher-avatar.jpg")
                .build();

        UserProfileResponse response = UserProfileResponse.builder()
                .id(teacherId)
                .email("teacher@test.com")
                .fullName("Dr. Teacher Updated")
                .phoneNumber("0988888888")
                .avatarUrl("https://example.com/teacher-avatar.jpg")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("TEACHER"))
                .build();

        when(teacherProfileService.updateMyProfile(eq(teacherId), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/teacher/profile")
                        .with(authentication(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Dr. Teacher Updated"))
                .andExpect(jsonPath("$.phoneNumber").value("0988888888"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/teacher-avatar.jpg"));
    }

    @Test
    @DisplayName("3. Unauthenticated user cannot access /api/v1/teacher/profile (returns 401)")
    void unauthenticated_cannotAccessTeacherProfile() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
