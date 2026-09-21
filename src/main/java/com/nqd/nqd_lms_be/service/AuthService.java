package com.nqd.nqd_lms_be.service;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.AuthUserResponse;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface AuthService {
    AppUserPrincipal processGoogleLogin(Map<String, Object> attributes, OidcIdToken idToken, OidcUserInfo userInfo);
    AuthUserResponse getCurrentUser(AppUserPrincipal principal);
    Set<String> getUserRoles(UUID userId);
    void completeOnboarding(UUID userId);
}
