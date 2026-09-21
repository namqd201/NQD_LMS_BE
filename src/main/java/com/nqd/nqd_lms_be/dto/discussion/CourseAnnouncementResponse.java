package com.nqd.nqd_lms_be.dto.discussion;

import com.nqd.nqd_lms_be.entity.CourseAnnouncement;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAnnouncementResponse {

    private UUID id;
    private UUID courseId;
    private UUID authorId;
    private String authorName;
    private String authorEmail;
    private String title;
    private String content;
    private LocalDateTime postedAt;

    public static CourseAnnouncementResponse fromEntity(CourseAnnouncement a) {
        if (a == null) return null;
        return CourseAnnouncementResponse.builder()
                .id(a.getId())
                .courseId(a.getCourse() != null ? a.getCourse().getId() : null)
                .authorId(a.getAuthor() != null ? a.getAuthor().getId() : null)
                .authorName(a.getAuthor() != null ? a.getAuthor().getFullName() : null)
                .authorEmail(a.getAuthor() != null ? a.getAuthor().getEmail() : null)
                .title(a.getTitle())
                .content(a.getContent())
                .postedAt(a.getPostedAt())
                .build();
    }
}
