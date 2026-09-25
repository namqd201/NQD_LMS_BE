package com.nqd.nqd_lms_be.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

public class ClassroomMaterialDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Tiêu đề tài liệu không được để trống")
        private String title;
        private String description;
        private String materialType; // PDF, SLIDE, TEXTBOOK, EXAM_PREP, LINK, OTHER
        @NotBlank(message = "Đường dẫn tài liệu không được để trống")
        private String fileUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID classroomId;
        private String title;
        private String description;
        private String materialType;
        private String fileUrl;
        private UUID uploadedById;
        private String uploadedByName;
        private Integer downloadCount;
        private LocalDateTime createdAt;
    }
}
