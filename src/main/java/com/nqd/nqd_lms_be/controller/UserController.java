package com.nqd.nqd_lms_be.controller;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.user.UpdateProfileRequest;
import com.nqd.nqd_lms_be.dto.user.UserProfileResponse;
import com.nqd.nqd_lms_be.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users - Shared", description = "Endpoints for shared user profile management")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(userService.getCurrentUserProfile(principal.getId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current authenticated user profile (SecurityContext identity enforced)")
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateCurrentUserProfile(principal.getId(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user profile by ID")
    public ResponseEntity<UserProfileResponse> getUserProfileById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(userService.getUserProfileById(id));
    }
}
