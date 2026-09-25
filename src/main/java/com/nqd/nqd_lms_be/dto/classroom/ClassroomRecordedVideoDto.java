package com.nqd.nqd_lms_be.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class ClassroomRecordedVideoDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Tiêu đề video không được để trống")
        private String title;
        @NotBlank(message = "Đường dẫn video không được để trống")
        private String videoUrl;
        private LocalDate sessionDate;
        private Integer durationMinutes;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID classroomId;
        private String title;
        private String videoUrl;
        private LocalDate sessionDate;
        private Integer durationMinutes;
        private String description;
        private UUID uploadedById;
        private String uploadedByName;
        private LocalDateTime createdAt;
    }
}
