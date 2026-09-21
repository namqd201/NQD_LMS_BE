package com.nqd.nqd_lms_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.admin.AdminUserResponse;
import com.nqd.nqd_lms_be.dto.admin.AssignRoleRequest;
import com.nqd.nqd_lms_be.dto.admin.UpdateUserStatusRequest;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AdminUserManagementTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

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
                .fullName("Admin Test")
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
    @DisplayName("1. Admin can search users by query and status")
    void admin_canSearchUsers() throws Exception {
        OAuth2AuthenticationToken adminToken = createAuthToken("admin@example.com", Set.of("ADMIN"));
        AdminUserResponse response = AdminUserResponse.builder()
                .id(UUID.randomUUID())
                .email("student@test.com")
                .fullName("John Doe")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("STUDENT"))
                .build();

        when(adminUserService.searchUsers(eq("John"), eq("ACTIVE"))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/admin/users?query=John&status=ACTIVE")
                        .with(authentication(adminToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("John Doe"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("2. Admin can view a specific user by ID")
    void admin_canViewUserById() throws Exception {
        OAuth2AuthenticationToken adminToken = createAuthToken("admin@example.com", Set.of("ADMIN"));
        UUID targetId = UUID.randomUUID();
        AdminUserResponse response = AdminUserResponse.builder()
                .id(targetId)
                .email("target@test.com")
                .fullName("Target User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("TEACHER"))
                .build();

        when(adminUserService.getUserById(eq(targetId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/users/" + targetId)
                        .with(authentication(adminToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("target@test.com"));
    }

    @Test
    @DisplayName("3. Admin can activate/deactivate user status")
    void admin_canUpdateUserStatus() throws Exception {
        OAuth2AuthenticationToken adminToken = createAuthToken("admin@example.com", Set.of("ADMIN"));
        UUID targetId = UUID.randomUUID();
        UpdateUserStatusRequest req = UpdateUserStatusRequest.builder()
                .status(UserStatus.INACTIVE)
                .build();

        AdminUserResponse response = AdminUserResponse.builder()
                .id(targetId)
                .email("target@test.com")
                .fullName("Target User")
                .status(UserStatus.INACTIVE)
                .roles(Set.of("STUDENT"))
                .build();

        when(adminUserService.updateUserStatus(eq(targetId), eq(req))).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/users/" + targetId + "/status")
                        .with(authentication(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("4. Admin can assign a single role to user")
    void admin_canAssignRole() throws Exception {
        OAuth2AuthenticationToken adminToken = createAuthToken("admin@example.com", Set.of("ADMIN"));
        UUID targetId = UUID.randomUUID();
        AssignRoleRequest req = AssignRoleRequest.builder()
                .roleName("TEACHER")
                .build();

        AdminUserResponse response = AdminUserResponse.builder()
                .id(targetId)
                .email("target@test.com")
                .roles(Set.of("STUDENT", "TEACHER"))
                .build();

        when(adminUserService.assignRole(eq(targetId), eq("TEACHER"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/roles")
                        .with(authentication(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    @DisplayName("5. Admin can remove a single role from user")
    void admin_canRemoveRole() throws Exception {
        OAuth2AuthenticationToken adminToken = createAuthToken("admin@example.com", Set.of("ADMIN"));
        UUID targetId = UUID.randomUUID();

        AdminUserResponse response = AdminUserResponse.builder()
                .id(targetId)
                .email("target@test.com")
                .roles(Set.of("STUDENT"))
                .build();

        when(adminUserService.removeRole(eq(targetId), eq("TEACHER"))).thenReturn(response);

        mockMvc.perform(delete("/api/v1/admin/users/" + targetId + "/roles/TEACHER")
                        .with(authentication(adminToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("6. Admin can view user subscription details")
    void admin_canGetUserSubscription() throws Exception {
        OAuth2AuthenticationToken adminToken = createAuthToken("admin@example.com", Set.of("ADMIN"));
        UUID targetId = UUID.randomUUID();

        com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse sub = com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse.builder()
                .userId(targetId)
                .planCode("VIP_STUDENT_MONTHLY")
                .planName("Gói Học Sinh VIP (Tháng)")
                .status(com.nqd.nqd_lms_be.entity.enums.SubscriptionStatus.ACTIVE)
                .isCurrentlyActive(true)
                .build();

        when(adminUserService.getUserSubscription(eq(targetId))).thenReturn(sub);

        mockMvc.perform(get("/api/v1/admin/users/" + targetId + "/subscription")
                        .with(authentication(adminToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("VIP_STUDENT_MONTHLY"))
                .andExpect(jsonPath("$.planName").value("Gói Học Sinh VIP (Tháng)"));
    }

    @Test
    @DisplayName("7. Admin can grant VIP/PRO free to student or teacher")
    void admin_canGrantUserVip() throws Exception {
        OAuth2AuthenticationToken adminToken = createAuthToken("admin@example.com", Set.of("ADMIN"));
        UUID targetId = UUID.randomUUID();

        com.nqd.nqd_lms_be.dto.admin.AdminGrantVipRequest grantReq = com.nqd.nqd_lms_be.dto.admin.AdminGrantVipRequest.builder()
                .planCode("TEACHER_PRO_YEARLY")
                .durationMonths(12)
                .reason("Giáo viên thử nghiệm dự án")
                .build();

        com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse sub = com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse.builder()
                .userId(targetId)
                .planCode("TEACHER_PRO_YEARLY")
                .planName("Gói Giáo Viên Pro (Năm)")
                .status(com.nqd.nqd_lms_be.entity.enums.SubscriptionStatus.ACTIVE)
                .isCurrentlyActive(true)
                .build();

        when(adminUserService.grantUserVip(eq(targetId), org.mockito.ArgumentMatchers.any())).thenReturn(sub);

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/vip")
                        .with(authentication(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grantReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("TEACHER_PRO_YEARLY"))
                .andExpect(jsonPath("$.planName").value("Gói Giáo Viên Pro (Năm)"));
    }

    @Test
    @DisplayName("8. Admin can revoke VIP/PRO subscription from user")
    void admin_canRevokeUserVip() throws Exception {
        OAuth2AuthenticationToken adminToken = createAuthToken("admin@example.com", Set.of("ADMIN"));
        UUID targetId = UUID.randomUUID();

        com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse fallbackSub = com.nqd.nqd_lms_be.membership.dto.SubscriptionResponse.builder()
                .userId(targetId)
                .planCode("FREE_STUDENT")
                .planName("Gói Học Sinh Miễn Phí")
                .status(com.nqd.nqd_lms_be.entity.enums.SubscriptionStatus.ACTIVE)
                .isCurrentlyActive(true)
                .build();

        when(adminUserService.revokeUserVip(eq(targetId), eq("Hết hạn ưu đãi"))).thenReturn(fallbackSub);

        mockMvc.perform(delete("/api/v1/admin/users/" + targetId + "/subscription?reason=Hết hạn ưu đãi")
                        .with(authentication(adminToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("FREE_STUDENT"));
    }
}
