package com.nqd.nqd_lms_be.util;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AppUserPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal;
        }
        throw new ForbiddenOperationException("User is not authenticated");
    }

    public static UUID getCurrentUserId() {
        return getCurrentPrincipal().getId();
    }

    public static String getCurrentUserEmail() {
        return getCurrentPrincipal().getEmail();
    }

    public static Set<String> getCurrentUserRoles() {
        AppUserPrincipal principal = getCurrentPrincipal();
        return principal != null ? principal.getRoles() : Collections.emptySet();
    }

    public static boolean hasRole(String roleName) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
                Set<String> roles = principal.getRoles();
                String normalizedRole = roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
                return roles.contains(normalizedRole) || roles.contains("ROLE_" + normalizedRole);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public static boolean isTeacher() {
        return hasRole("TEACHER");
    }

    public static boolean isStudent() {
        return hasRole("STUDENT");
    }
}
