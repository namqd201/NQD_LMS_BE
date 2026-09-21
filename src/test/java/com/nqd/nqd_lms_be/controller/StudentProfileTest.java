package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.user.UpdateProfileRequest;
import com.nqd.nqd_lms_be.dto.user.UserProfileResponse;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.service.student.StudentProfileService;
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
class StudentProfileTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    @MockitoBean
    private StudentProfileService studentProfileService;

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
    @DisplayName("1. Student can view own profile via GET /api/v1/student/profile")
    void student_canViewOwnProfile() throws Exception {
        UUID studentId = UUID.randomUUID();
        OAuth2AuthenticationToken studentToken = createAuthToken(studentId, "student@test.com", Set.of("STUDENT"));

        UserProfileResponse response = UserProfileResponse.builder()
                .id(studentId)
                .email("student@test.com")
                .fullName("Student User")
                .phoneNumber("0987654321")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("STUDENT"))
                .build();

        when(studentProfileService.getMyProfile(eq(studentId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/student/profile")
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@test.com"))
                .andExpect(jsonPath("$.fullName").value("Student User"))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"));
    }

    @Test
    @DisplayName("2. Student can update allowed profile fields (fullName, phoneNumber, avatarUrl)")
    void student_canUpdateAllowedProfileFields() throws Exception {
        UUID studentId = UUID.randomUUID();
        OAuth2AuthenticationToken studentToken = createAuthToken(studentId, "student@test.com", Set.of("STUDENT"));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Updated Student Name")
                .phoneNumber("0123456789")
                .avatarUrl("https://example.com/new-avatar.png")
                .build();

        UserProfileResponse response = UserProfileResponse.builder()
                .id(studentId)
                .email("student@test.com")
                .fullName("Updated Student Name")
                .phoneNumber("0123456789")
                .avatarUrl("https://example.com/new-avatar.png")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("STUDENT"))
                .build();

        when(studentProfileService.updateMyProfile(eq(studentId), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/student/profile")
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Student Name"))
                .andExpect(jsonPath("$.phoneNumber").value("0123456789"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/new-avatar.png"));
    }

    @Test
    @DisplayName("3. Unauthenticated user cannot access /api/v1/student/profile (returns 401)")
    void unauthenticated_cannotAccessStudentProfile() throws Exception {
        mockMvc.perform(get("/api/v1/student/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
