package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.user.UpdateProfileRequest;
import com.nqd.nqd_lms_be.dto.user.UserProfileResponse;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.service.UserService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class UserProfileOwnershipSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

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
                .fullName("Authenticated User")
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
    @DisplayName("1. User accessing GET /api/v1/users/me uses SecurityContext userId")
    void user_canGetOwnProfileViaSharedController() throws Exception {
        UUID authenticatedUserId = UUID.randomUUID();
        OAuth2AuthenticationToken authToken = createAuthToken(authenticatedUserId, "user@test.com", Set.of("STUDENT"));

        UserProfileResponse response = UserProfileResponse.builder()
                .id(authenticatedUserId)
                .email("user@test.com")
                .fullName("Authenticated User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("STUDENT"))
                .build();

        when(userService.getCurrentUserProfile(eq(authenticatedUserId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(authenticatedUserId.toString()))
                .andExpect(jsonPath("$.email").value("user@test.com"));

        verify(userService).getCurrentUserProfile(eq(authenticatedUserId));
    }

    @Test
    @DisplayName("2. User updating profile via PUT /api/v1/users/me always passes SecurityContext userId to service")
    void user_updatingProfile_strictlyUsesSecurityContextPrincipalId() throws Exception {
        UUID authenticatedUserId = UUID.randomUUID();
        OAuth2AuthenticationToken authToken = createAuthToken(authenticatedUserId, "user@test.com", Set.of("STUDENT"));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("New Name")
                .phoneNumber("0999999999")
                .build();

        UserProfileResponse response = UserProfileResponse.builder()
                .id(authenticatedUserId)
                .email("user@test.com")
                .fullName("New Name")
                .phoneNumber("0999999999")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("STUDENT"))
                .build();

        when(userService.updateCurrentUserProfile(eq(authenticatedUserId), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Name"));

        // Verify strictly that authenticatedUserId was passed, never an arbitrary ID
        verify(userService).updateCurrentUserProfile(eq(authenticatedUserId), any(UpdateProfileRequest.class));
    }
}
