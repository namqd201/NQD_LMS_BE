package com.nqd.nqd_lms_be.service;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.AuthUserResponse;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.repository.OAuthAccountRepository;
import com.nqd.nqd_lms_be.repository.RoleRepository;
import com.nqd.nqd_lms_be.repository.UserRepository;
import com.nqd.nqd_lms_be.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    public static final String DEFAULT_ROLE_NAME = "STUDENT";
    public static final String GOOGLE_PROVIDER = "GOOGLE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer kafkaNotificationProducer;

    @Override
    @Transactional
    public AppUserPrincipal processGoogleLogin(Map<String, Object> attributes, OidcIdToken idToken, OidcUserInfo userInfo) {
        String sub = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_google_user"),
                    "Email not found in Google OAuth response"
            );
        }

        if (sub == null || sub.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_google_user"),
                    "Google subject ID (sub) not found"
            );
        }

        log.info("Processing Google OAuth login for email: {}", email);

        // 1. Check if OAuth account exists for this Google sub
        Optional<OAuthAccount> existingOAuth = oauthAccountRepository.findByProviderAndProviderUserId(GOOGLE_PROVIDER, sub);
        User user;

        if (existingOAuth.isPresent()) {
            user = existingOAuth.get().getUser();
            updateUserLoginInfo(user, name, picture);
        } else {
            // 2. Check if user exists by email
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent()) {
                user = existingUser.get();
                updateUserLoginInfo(user, name, picture);
                linkGoogleAccount(user, sub, email);
            } else {
                // 3. Provision new user for first-time Google login
                user = provisionNewUser(email, name, picture, sub);
            }
        }

        Set<String> roles = getUserRoles(user.getId());
        return AppUserPrincipal.create(user, roles, attributes, idToken, userInfo);
    }

    private void updateUserLoginInfo(User user, String name, String picture) {
        user.setLastLoginAt(LocalDateTime.now());
        // Do NOT overwrite customized full name or avatar if already present in DB
        if ((user.getFullName() == null || user.getFullName().isBlank()) && name != null && !name.isBlank()) {
            user.setFullName(name);
        }
        if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && picture != null && !picture.isBlank()) {
            user.setAvatarUrl(picture);
        }
        userRepository.save(user);
    }

    private void linkGoogleAccount(User user, String sub, String email) {
        OAuthAccount oAuthAccount = OAuthAccount.builder()
                .user(user)
                .provider(GOOGLE_PROVIDER)
                .providerUserId(sub)
                .providerEmail(email)
                .build();
        oauthAccountRepository.save(oAuthAccount);
        log.info("Linked Google OAuth account (sub={}) to existing user: {}", sub, user.getEmail());
    }

    private User provisionNewUser(String email, String name, String picture, String sub) {
        log.info("Provisioning new user for first-time Google login: {}", email);

        User newUser = User.builder()
                .email(email)
                .fullName(name != null && !name.isBlank() ? name : email)
                .avatarUrl(picture)
                .status(UserStatus.ACTIVE)
                .lastLoginAt(LocalDateTime.now())
                .build();
        newUser = userRepository.save(newUser);

        // Assign default STUDENT role
        Role studentRole = roleRepository.findByName(DEFAULT_ROLE_NAME)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(DEFAULT_ROLE_NAME)
                        .description("Student / learner")
                        .build()));

        UserRole userRole = UserRole.builder()
                .userId(newUser.getId())
                .roleId(studentRole.getId())
                .user(newUser)
                .role(studentRole)
                .createdAt(LocalDateTime.now())
                .build();
        userRoleRepository.save(userRole);

        // Create OAuthAccount record
        linkGoogleAccount(newUser, sub, email);

        // Send Kafka Welcome Notification
        kafkaNotificationProducer.sendNotification(
                newUser.getId(),
                "WELCOME",
                "Chào mừng bạn đến với NQD LMS!",
                "Tài khoản của bạn đã được khởi tạo thành công. Hãy bắt đầu khám phá các khóa học và bài học thú vị ngay nhé!",
                "/courses"
        );

        return newUser;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUserResponse getCurrentUser(AppUserPrincipal principal) {
        if (principal == null) {
            return null;
        }
        // Always fetch the freshest user data directly from the database
        Optional<User> userOpt = userRepository.findById(principal.getId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Set<String> roles = getUserRoles(user.getId());
            boolean isStaffOrTeacher = roles.stream().anyMatch(r -> 
                    r.equalsIgnoreCase("TEACHER") || r.equalsIgnoreCase("ROLE_TEACHER") ||
                    r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ROLE_ADMIN"));
            boolean isOnboarded = isStaffOrTeacher || Boolean.TRUE.equals(user.getIsOnboarded());

            return AuthUserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .avatarUrl(user.getAvatarUrl())
                    .phoneNumber(user.getPhoneNumber())
                    .status(user.getStatus())
                    .roles(roles)
                    .isOnboarded(isOnboarded)
                    .build();
        }

        Set<String> roles = getUserRoles(principal.getId());
        boolean isStaffOrTeacher = roles.stream().anyMatch(r -> 
                r.equalsIgnoreCase("TEACHER") || r.equalsIgnoreCase("ROLE_TEACHER") ||
                r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ROLE_ADMIN"));
        return AuthUserResponse.builder()
                .id(principal.getId())
                .email(principal.getEmail())
                .fullName(principal.getFullName())
                .avatarUrl(principal.getAvatarUrl())
                .phoneNumber(principal.getPhoneNumber())
                .status(principal.getStatus())
                .roles(roles)
                .isOnboarded(isStaffOrTeacher)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getUserRoles(UUID userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        List<String> roles = userRoleRepository.findRoleNamesByUserId(userId);
        return new HashSet<>(roles);
    }

    @Override
    @Transactional
    public void completeOnboarding(UUID userId) {
        if (userId == null) return;
        userRepository.findById(userId).ifPresent(user -> {
            user.setIsOnboarded(true);
            userRepository.save(user);
            log.info("User {} marked as onboarded successfully", userId);
        });
    }
}
