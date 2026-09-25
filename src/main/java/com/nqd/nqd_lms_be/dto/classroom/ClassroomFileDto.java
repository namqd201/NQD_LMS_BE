package com.nqd.nqd_lms_be.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

public class ClassroomFileDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Tên tệp tin không được để trống")
        private String fileName;
        @NotBlank(message = "Đường dẫn tệp tin không được để trống")
        private String fileUrl;
        private Long fileSize;
        private String fileType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID classroomId;
        private String fileName;
        private String fileUrl;
        private Long fileSize;
        private String fileType;
        private UUID uploadedById;
        private String uploadedByName;
        private LocalDateTime createdAt;
    }
}
