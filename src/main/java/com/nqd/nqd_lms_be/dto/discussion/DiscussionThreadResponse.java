package com.nqd.nqd_lms_be.dto.discussion;

import com.nqd.nqd_lms_be.entity.DiscussionThread;
import com.nqd.nqd_lms_be.entity.enums.DiscussionThreadStatus;
import com.nqd.nqd_lms_be.entity.enums.PostReactionType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
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
    private String authorAvatarUrl;
    private String title;
    private String content;
    private Boolean isPinned;
    private Boolean isLocked;
    private DiscussionThreadStatus status;
    private Integer postCount;
    private Integer viewCount;
    private Integer reactionCount;
    private PostReactionType myReaction;
    private Map<String, Integer> reactionBreakdown;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DiscussionThreadResponse fromEntity(DiscussionThread t) {
        return fromEntity(t, null, null, Collections.emptyMap(), 0);
    }

    public static DiscussionThreadResponse fromEntity(
            DiscussionThread t,
            UUID currentUserId,
            PostReactionType myReaction,
            Map<String, Integer> reactionBreakdown,
            int reactionCount
    ) {
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
                .authorAvatarUrl(t.getAuthor() != null ? t.getAuthor().getAvatarUrl() : null)
                .title(t.getTitle())
                .content(t.getContent())
                .isPinned(t.getIsPinned())
                .isLocked(t.getIsLocked())
                .status(t.getStatus())
                .postCount(t.getPostCount())
                .viewCount(t.getViewCount())
                .reactionCount(reactionCount)
                .myReaction(myReaction)
                .reactionBreakdown(reactionBreakdown != null ? reactionBreakdown : Collections.emptyMap())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
