package com.nqd.nqd_lms_be.controller.slide;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.slide.GenerateSlideRequest;
import com.nqd.nqd_lms_be.dto.slide.LessonSlideResponse;
import com.nqd.nqd_lms_be.entity.enums.SlideTargetType;
import com.nqd.nqd_lms_be.service.slide.LessonSlideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/slides")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lesson Slides", description = "Endpoints for viewing, generating with AI, uploading, and exporting lesson slides")
public class LessonSlideController {

    private final LessonSlideService lessonSlideService;
    private static final String UPLOAD_SLIDES_DIR = "uploads/slides";

    @GetMapping
    @Operation(summary = "Get slide details and slide pages for a lesson")
    public ResponseEntity<LessonSlideResponse> getSlide(
            @RequestParam SlideTargetType targetType,
            @RequestParam UUID targetId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(lessonSlideService.getSlide(targetType, targetId, currentUserId));
    }

    @PostMapping("/ai-generate")
    @Operation(summary = "Generate presentation slides automatically using Gemini AI (Requires Admin or Teacher PRO)")
    public ResponseEntity<LessonSlideResponse> generateSlideWithAi(
            @Valid @RequestBody GenerateSlideRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(lessonSlideService.generateSlideWithAi(request, currentUserId));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload presentation slide file (.pptx, .ppt, .pdf) manually")
    public ResponseEntity<LessonSlideResponse> uploadSlideFile(
            @RequestParam SlideTargetType targetType,
            @RequestParam UUID targetId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(lessonSlideService.uploadSlideFile(targetType, targetId, file, currentUserId));
    }

    @DeleteMapping("/{slideId}")
    @Operation(summary = "Delete an existing slide for a lesson")
    public ResponseEntity<MessageResponse> deleteSlide(
            @PathVariable UUID slideId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        lessonSlideService.deleteSlide(slideId, currentUserId);
        return ResponseEntity.ok(MessageResponse.of("Xóa slide bài học thành công."));
    }

    @GetMapping("/download")
    @Operation(summary = "Download generated PowerPoint (.pptx) presentation for lesson")
    public ResponseEntity<Resource> downloadPptx(
            @RequestParam SlideTargetType targetType,
            @RequestParam UUID targetId
    ) {
        byte[] pptxBytes = lessonSlideService.exportPptx(targetType, targetId);
        ByteArrayResource resource = new ByteArrayResource(pptxBytes);

        String filename = "slide-" + targetType.name().toLowerCase() + "-" + targetId.toString().substring(0, 8) + ".pptx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
                .contentLength(pptxBytes.length)
                .body(resource);
    }

    @GetMapping("/files/{filename}")
    @Operation(summary = "Get uploaded slide file directly")
    public ResponseEntity<Resource> getSlideFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_SLIDES_DIR).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = "application/octet-stream";
                if (filename.toLowerCase().endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (filename.toLowerCase().endsWith(".pptx")) {
                    contentType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                } else if (filename.toLowerCase().endsWith(".ppt")) {
                    contentType = "application/vnd.ms-powerpoint";
                }
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
