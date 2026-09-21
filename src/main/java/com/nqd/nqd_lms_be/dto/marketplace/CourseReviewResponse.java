package com.nqd.nqd_lms_be.dto.marketplace;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReviewResponse {
    private UUID id;
    private UUID courseId;
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private String userAvatarUrl;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
