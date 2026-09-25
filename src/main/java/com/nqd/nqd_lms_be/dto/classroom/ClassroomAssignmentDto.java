package com.nqd.nqd_lms_be.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

public class ClassroomAssignmentDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Tiêu đề bài tập không được để trống")
        private String title;
        private String description;
        private LocalDateTime deadline;
        private Integer maxScore;
        private String attachmentUrl;
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
        private LocalDateTime deadline;
        private Integer maxScore;
        private String attachmentUrl;
        private String status;
        private UUID assignedById;
        private String assignedByName;
        private LocalDateTime createdAt;
    }
}
