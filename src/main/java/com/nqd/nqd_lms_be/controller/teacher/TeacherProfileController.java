package com.nqd.nqd_lms_be.controller.teacher;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.user.UpdateProfileRequest;
import com.nqd.nqd_lms_be.dto.user.UserProfileResponse;
import com.nqd.nqd_lms_be.service.teacher.TeacherProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teacher/profile")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher - Profile", description = "Endpoints for teachers to view and update their own profile")
public class TeacherProfileController {

    private final TeacherProfileService teacherProfileService;

    @GetMapping
    @Operation(summary = "Get current teacher profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(teacherProfileService.getMyProfile(principal.getId()));
    }

    @PutMapping
    @Operation(summary = "Update allowed profile fields for current teacher (SecurityContext enforced)")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(teacherProfileService.updateMyProfile(principal.getId(), request));
    }
}
