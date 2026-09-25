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
        @NotBlank(message = "Tiêu đề bài học không được để trống")
        private String title;
        private String chapterTitle;
        private Integer lessonOrder;
        private String description;
        private String content;
        private String videoUrl;
        private String materialType; // LESSON, PDF, SLIDE, TEXTBOOK, EXAM_PREP, LINK, OTHER
        private String fileUrl;
        private String attachmentName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID classroomId;
        private String title;
        private String chapterTitle;
        private Integer lessonOrder;
        private String description;
        private String content;
        private String videoUrl;
        private String materialType;
        private String fileUrl;
        private String attachmentName;
        private UUID uploadedById;
        private String uploadedByName;
        private Integer downloadCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
