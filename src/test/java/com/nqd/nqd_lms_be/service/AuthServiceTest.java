package com.nqd.nqd_lms_be.service;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.repository.OAuthAccountRepository;
import com.nqd.nqd_lms_be.repository.RoleRepository;
import com.nqd.nqd_lms_be.repository.UserRepository;
import com.nqd.nqd_lms_be.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OAuthAccountRepository oauthAccountRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByName("STUDENT").isEmpty()) {
            roleRepository.save(Role.builder().name("STUDENT").description("Student / learner").build());
        }
        if (roleRepository.findByName("TEACHER").isEmpty()) {
            roleRepository.save(Role.builder().name("TEACHER").description("Teacher / instructor").build());
        }
        if (roleRepository.findByName("ADMIN").isEmpty()) {
            roleRepository.save(Role.builder().name("ADMIN").description("System administrator").build());
        }
    }

    @Test
    @DisplayName("Provision new Google user with default STUDENT role and OAuthAccount")
    void provisionNewGoogleUser_createsUserWithDefaultStudentRoleAndOAuthAccount() {
        Map<String, Object> attributes = Map.of(
                "sub", "google-sub-001",
                "email", "newstudent@example.com",
                "name", "New Student",
                "picture", "https://example.com/avatar.jpg"
        );

        AppUserPrincipal principal = authService.processGoogleLogin(attributes, null, null);

        assertThat(principal).isNotNull();
        assertThat(principal.getEmail()).isEqualTo("newstudent@example.com");
        assertThat(principal.getFullName()).isEqualTo("New Student");
        assertThat(principal.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
        assertThat(principal.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(principal.getRoles()).containsExactly("STUDENT");

        Optional<User> savedUser = userRepository.findByEmail("newstudent@example.com");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getLastLoginAt()).isNotNull();

        Optional<OAuthAccount> oAuthAccount = oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-sub-001");
        assertThat(oAuthAccount).isPresent();
        assertThat(oAuthAccount.get().getUser().getId()).isEqualTo(savedUser.get().getId());
    }

    @Test
    @DisplayName("Existing Google user is not duplicated upon subsequent login")
    void existingGoogleUser_isNotDuplicated() {
        Map<String, Object> attributes = Map.of(
                "sub", "google-sub-002",
                "email", "repeatuser@example.com",
                "name", "Repeat User",
                "picture", "https://example.com/avatar1.jpg"
        );

        AppUserPrincipal firstPrincipal = authService.processGoogleLogin(attributes, null, null);
        long initialUserCount = userRepository.count();

        Map<String, Object> secondAttributes = Map.of(
                "sub", "google-sub-002",
                "email", "repeatuser@example.com",
                "name", "Repeat User Updated",
                "picture", "https://example.com/avatar2.jpg"
        );
        AppUserPrincipal secondPrincipal = authService.processGoogleLogin(secondAttributes, null, null);

        assertThat(secondPrincipal.getId()).isEqualTo(firstPrincipal.getId());
        assertThat(userRepository.count()).isEqualTo(initialUserCount);

        User updatedUser = userRepository.findById(firstPrincipal.getId()).orElseThrow();
        assertThat(updatedUser.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("Existing roles (e.g. TEACHER) are strictly preserved on Google login")
    void existingRolesArePreserved_whenGoogleUserLogsIn() {
        User teacher = userRepository.save(User.builder()
                .email("teacher@school.edu")
                .fullName("Master Teacher")
                .status(UserStatus.ACTIVE)
                .build());

        Role teacherRole = roleRepository.findByName("TEACHER").orElseThrow();
        userRoleRepository.save(UserRole.builder()
                .userId(teacher.getId())
                .roleId(teacherRole.getId())
                .user(teacher)
                .role(teacherRole)
                .build());

        Map<String, Object> attributes = Map.of(
                "sub", "google-teacher-sub-999",
                "email", "teacher@school.edu",
                "name", "Master Teacher",
                "picture", "https://example.com/teacher.jpg"
        );

        AppUserPrincipal principal = authService.processGoogleLogin(attributes, null, null);

        assertThat(principal.getRoles()).containsExactly("TEACHER");
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_TEACHER");
    }

    @Test
    @DisplayName("Google login cannot arbitrarily assign ADMIN role via token claims or payload")
    void googleLogin_cannotArbitrarilyAssignAdminRole() {
        Map<String, Object> rogueAttributes = Map.of(
                "sub", "google-attacker-sub",
                "email", "attacker@example.com",
                "name", "Attacker",
                "role", "ADMIN",
                "roles", List.of("ADMIN", "SUPERADMIN"),
                "is_admin", true
        );

        AppUserPrincipal principal = authService.processGoogleLogin(rogueAttributes, null, null);

        assertThat(principal.getRoles()).doesNotContain("ADMIN");
        assertThat(principal.getRoles()).containsExactly("STUDENT");
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .doesNotContain("ROLE_ADMIN");
    }
}
