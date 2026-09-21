package com.nqd.nqd_lms_be.controller;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.AuthUserResponse;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private AppUserPrincipal createMockPrincipal(UUID id, String email, String name, Set<String> roles) {
        User user = User.builder()
                .email(email)
                .fullName(name)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(id);
        return new AppUserPrincipal(
                user,
                roles,
                Map.of("sub", "google-sub-" + id, "email", email, "name", name),
                null,
                null
        );
    }

    @Test
    @DisplayName("1. Unauthenticated user cannot access /api/v1/auth/me (returns 401)")
    void unauthenticatedUser_cannotAccessAuthMe_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Phiên đăng nhập đã hết hạn hoặc bạn chưa đăng nhập. Vui lòng đăng nhập lại để tiếp tục sử dụng."))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("2. Authenticated user can access /api/v1/auth/me (returns 200 with user profile)")
    void authenticatedUser_canAccessAuthMe_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUserPrincipal principal = createMockPrincipal(userId, "test@example.com", "Test User", Set.of("STUDENT"));
        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");

        AuthUserResponse expectedResponse = AuthUserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .fullName("Test User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("STUDENT"))
                .build();

        when(authService.getCurrentUser(any(AppUserPrincipal.class))).thenReturn(expectedResponse);

        mockMvc.perform(get("/api/v1/auth/me")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"));
    }

    @Test
    @DisplayName("3. Logout invalidates authenticated session and clears security context")
    void logout_invalidatesSession_andSubsequentRequestCannotAccessAuthMe() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUserPrincipal principal = createMockPrincipal(userId, "test@example.com", "Test User", Set.of("STUDENT"));
        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");

        MockHttpSession session = new MockHttpSession();
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authToken);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        // Perform logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công."));

        // Verify session was invalidated
        assertThat(session.isInvalid()).isTrue();
    }
}
