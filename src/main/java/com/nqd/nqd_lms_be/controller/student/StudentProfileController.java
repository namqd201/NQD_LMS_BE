package com.nqd.nqd_lms_be.controller.student;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.user.UpdateProfileRequest;
import com.nqd.nqd_lms_be.dto.user.UserProfileResponse;
import com.nqd.nqd_lms_be.service.student.StudentProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student/profile")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
@Tag(name = "Student - Profile", description = "Endpoints for students to view and update their own profile")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @GetMapping
    @Operation(summary = "Get current student profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(studentProfileService.getMyProfile(principal.getId()));
    }

    @PutMapping
    @Operation(summary = "Update allowed profile fields for current student (SecurityContext enforced)")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(studentProfileService.updateMyProfile(principal.getId(), request));
    }
}
