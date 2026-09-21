package com.nqd.nqd_lms_be.dto.discussion;

import com.nqd.nqd_lms_be.entity.DiscussionThread;
import com.nqd.nqd_lms_be.entity.enums.DiscussionThreadStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscussionThreadResponse {

    private UUID id;
    private UUID courseId;
    private UUID lessonId;
    private String lessonTitle;
    private UUID authorId;
    private String authorName;
    private String authorEmail;
    private String authorRole;
    private String title;
    private String content;
    private Boolean isPinned;
    private Boolean isLocked;
    private DiscussionThreadStatus status;
    private Integer postCount;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DiscussionThreadResponse fromEntity(DiscussionThread t) {
        if (t == null) return null;
        String authorRole = "STUDENT";
        if (t.getCourse() != null && t.getCourse().getCreator() != null && t.getAuthor() != null
                && t.getCourse().getCreator().getId().equals(t.getAuthor().getId())) {
            authorRole = "TEACHER";
        }

        return DiscussionThreadResponse.builder()
                .id(t.getId())
                .courseId(t.getCourse() != null ? t.getCourse().getId() : null)
                .lessonId(t.getLesson() != null ? t.getLesson().getId() : null)
                .lessonTitle(t.getLesson() != null ? t.getLesson().getTitle() : null)
                .authorId(t.getAuthor() != null ? t.getAuthor().getId() : null)
                .authorName(t.getAuthor() != null ? t.getAuthor().getFullName() : null)
                .authorEmail(t.getAuthor() != null ? t.getAuthor().getEmail() : null)
                .authorRole(authorRole)
                .title(t.getTitle())
                .content(t.getContent())
                .isPinned(t.getIsPinned())
                .isLocked(t.getIsLocked())
                .status(t.getStatus())
                .postCount(t.getPostCount())
                .viewCount(t.getViewCount())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
