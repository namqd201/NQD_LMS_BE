package com.nqd.nqd_lms_be.controller;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.TeacherApplicationRequest;
import com.nqd.nqd_lms_be.dto.TeacherApplicationResponse;
import com.nqd.nqd_lms_be.service.teacher.TeacherApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teacher-applications")
@RequiredArgsConstructor
@Tag(name = "Teacher Applications", description = "Endpoints for students/instructors to submit verification documents to become a Teacher")
public class TeacherApplicationController {

    private final TeacherApplicationService applicationService;

    @PostMapping("/upload-document")
    @Operation(summary = "Upload verification document (Degree, Student ID, Certificate, ID Card)")
    public ResponseEntity<Map<String, String>> uploadDocument(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String fileUrl = applicationService.uploadVerificationDocument(principal.getId(), file);
        return ResponseEntity.ok(Map.of("url", fileUrl));
    }

    @GetMapping("/documents/{filename:.+}")
    @Operation(summary = "Serve uploaded verification document")
    public ResponseEntity<Resource> serveDocument(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads/teacher-docs").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    @Operation(summary = "Submit new teacher verification application")
    public ResponseEntity<TeacherApplicationResponse> submitApplication(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody TeacherApplicationRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        TeacherApplicationResponse response = applicationService.submitApplication(principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-application")
    @Operation(summary = "Get current user's teacher application status")
    public ResponseEntity<TeacherApplicationResponse> getMyApplication(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        TeacherApplicationResponse response = applicationService.getMyApplication(principal.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/my-application")
    @Operation(summary = "Update and re-submit teacher application after rejection")
    public ResponseEntity<TeacherApplicationResponse> updateMyApplication(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody TeacherApplicationRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        TeacherApplicationResponse response = applicationService.updateMyApplication(principal.getId(), request);
        return ResponseEntity.ok(response);
    }
}
