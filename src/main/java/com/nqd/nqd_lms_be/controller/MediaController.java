package com.nqd.nqd_lms_be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@Tag(name = "Media Management", description = "Endpoints for uploading and serving media assets (images, diagrams)")
public class MediaController {

    private static final String UPLOAD_DIR = "uploads/media";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "svg");

    @PostMapping("/api/v1/media/upload")
    @Operation(summary = "Upload image/diagram for lessons, exercises, questions, or essays")
    public ResponseEntity<?> uploadMedia(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tập tin tải lên không được để trống", "success", false));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dung lượng hình ảnh không được vượt quá 10MB", "success", false));
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Định dạng hình ảnh không hợp lệ. Chỉ chấp nhận JPG, PNG, WEBP, GIF, hoặc SVG",
                    "success", false
            ));
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String storedFilename = UUID.randomUUID().toString() + "." + extension;
            Path targetLocation = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/api/v1/public/media/" + storedFilename;
            log.info("Uploaded media file: {} -> {}", originalFilename, fileUrl);

            return ResponseEntity.ok(Map.of(
                    "url", fileUrl,
                    "filename", originalFilename != null ? originalFilename : storedFilename,
                    "size", file.getSize(),
                    "success", true
            ));
        } catch (IOException e) {
            log.error("Failed to store media file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể lưu hình ảnh: " + e.getMessage(), "success", false));
        }
    }

    @GetMapping("/api/v1/public/media/{filename:.+}")
    @Operation(summary = "Serve uploaded media file")
    public ResponseEntity<Resource> serveMedia(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                if (filename.endsWith(".svg")) {
                    contentType = "image/svg+xml";
                } else if (filename.endsWith(".webp")) {
                    contentType = "image/webp";
                } else {
                    contentType = "application/octet-stream";
                }
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IOException e) {
            log.error("Error serving media: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/v1/public/classroom-files/{filename:.+}")
    @Operation(summary = "Serve uploaded classroom file for download/preview")
    public ResponseEntity<Resource> serveClassroomFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads/classroom_files").resolve(filename).normalize();
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
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IOException e) {
            log.error("Error serving classroom file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}