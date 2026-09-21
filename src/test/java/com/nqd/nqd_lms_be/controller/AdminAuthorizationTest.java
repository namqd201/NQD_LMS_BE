package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.admin.AdminUserResponse;
import com.nqd.nqd_lms_be.dto.admin.RoleResponse;
import com.nqd.nqd_lms_be.dto.admin.UpdateUserRoleRequest;
import com.nqd.nqd_lms_be.dto.admin.UpdateUserStatusRequest;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.service.admin.AdminRoleService;
import com.nqd.nqd_lms_be.service.admin.AdminUserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AdminAuthorizationTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private AdminRoleService adminRoleService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private OAuth2AuthenticationToken createAuthToken(String email, Set<String> roles) {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .email(email)
                .fullName("Test User")
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
    @DisplayName("1. ADMIN role can access GET /api/v1/admin/users (returns 200)")
    void admin_canAccessAdminUsers() throws Exception {
        OAuth2AuthenticationToken adminToken = createAuthToken("admin@example.com", Set.of("ADMIN"));
        AdminUserResponse response = AdminUserResponse.builder()
                .id(UUID.randomUUID())
                .email("student@example.com")
                .fullName("Student A")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("STUDENT"))
                .build();

        when(adminUserService.getAllUsers()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(authentication(adminToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("student@example.com"));
    }

    @Test
    @DisplayName("2. TEACHER attempting to access /api/v1/admin/users is denied (returns 403)")
    void teacher_cannotAccessAdminUsers_returns403() throws Exception {
        OAuth2AuthenticationToken teacherToken = createAuthToken("teacher@example.com", Set.of("TEACHER"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(authentication(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập chức năng này."))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("3. STUDENT attempting to access /api/v1/admin/users is denied (returns 403)")
    void student_cannotAccessAdminUsers_returns403() throws Exception {
        OAuth2AuthenticationToken studentToken = createAuthToken("student@example.com", Set.of("STUDENT"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(authentication(studentToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập chức năng này."))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("4. Unauthenticated user accessing /api/v1/admin/users returns 401 Unauthorized")
    void unauthenticated_cannotAccessAdminUsers_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Phiên đăng nhập đã hết hạn hoặc bạn chưa đăng nhập. Vui lòng đăng nhập lại để tiếp tục sử dụng."));
    }

    @Test
    @DisplayName("5. ADMIN role can access GET /api/v1/admin/roles (returns 200)")
    void admin_canAccessAdminRoles() throws Exception {
        OAuth2AuthenticationToken adminToken = createAuthToken("admin@example.com", Set.of("ADMIN"));
        com.nqd.nqd_lms_be.dto.admin.RoleDetailResponse role = com.nqd.nqd_lms_be.dto.admin.RoleDetailResponse.builder()
                .id(UUID.randomUUID())
                .name("TEACHER")
                .description("Teacher role")
                .userCount(5)
                .build();

        when(adminRoleService.getAllRoles()).thenReturn(List.of(role));

        mockMvc.perform(get("/api/v1/admin/roles")
                        .with(authentication(adminToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("TEACHER"));
    }

    @Test
    @DisplayName("6. ADMIN can update user roles via PUT /api/v1/admin/users/{id}/roles")
    void admin_canUpdateUserRoles() throws Exception {
        OAuth2AuthenticationToken adminToken = createAuthToken("admin@example.com", Set.of("ADMIN"));
        UUID targetUserId = UUID.randomUUID();
        UpdateUserRoleRequest req = UpdateUserRoleRequest.builder()
                .roles(Set.of("TEACHER"))
                .build();

        AdminUserResponse response = AdminUserResponse.builder()
                .id(targetUserId)
                .email("user@example.com")
                .fullName("User Name")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("TEACHER"))
                .build();

        when(adminUserService.updateUserRoles(eq(targetUserId), any(UpdateUserRoleRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/users/" + targetUserId + "/roles")
                        .with(authentication(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("TEACHER"));
    }

    @Test
    @DisplayName("7. TEACHER or STUDENT attempting to update user roles is denied (returns 403)")
    void teacherOrStudent_cannotUpdateUserRoles_returns403() throws Exception {
        OAuth2AuthenticationToken teacherToken = createAuthToken("teacher@example.com", Set.of("TEACHER"));
        UUID targetUserId = UUID.randomUUID();
        UpdateUserRoleRequest req = UpdateUserRoleRequest.builder()
                .roles(Set.of("ADMIN"))
                .build();

        mockMvc.perform(put("/api/v1/admin/users/" + targetUserId + "/roles")
                        .with(authentication(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
