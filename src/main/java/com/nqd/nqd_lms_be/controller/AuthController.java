package com.nqd.nqd_lms_be.controller;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.AuthUserResponse;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for Google OAuth2 Authentication & Session Management")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/me")
    @Operation(summary = "Get currently authenticated user information")
    public ResponseEntity<AuthUserResponse> getCurrentUser(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        AuthUserResponse response = authService.getCurrentUser(principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current user session")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, null);
        return ResponseEntity.ok(MessageResponse.of("Logged out successfully"));
    }

    @PostMapping("/complete-onboarding")
    @Operation(summary = "Mark user as having completed initial role onboarding")
    public ResponseEntity<MessageResponse> completeOnboarding(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        authService.completeOnboarding(principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Onboarding completed successfully"));
    }
}
