package com.nqd.nqd_lms_be.dto.discussion;

import com.nqd.nqd_lms_be.entity.DiscussionPost;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscussionPostResponse {

    private UUID id;
    private UUID threadId;
    private UUID parentId;
    private UUID authorId;
    private String authorName;
    private String authorEmail;
    private String authorRole;
    private String authorAvatarUrl;
    private String content;
    private Boolean isAnswer;
    private Integer upvoteCount;
    private Boolean isUpvotedByMe;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DiscussionPostResponse fromEntity(DiscussionPost p) {
        return fromEntity(p, null, false);
    }

    public static DiscussionPostResponse fromEntity(DiscussionPost p, boolean isUpvotedByMe) {
        return fromEntity(p, null, isUpvotedByMe);
    }

    public static DiscussionPostResponse fromEntity(DiscussionPost p, UUID currentUserId, boolean isUpvotedByMe) {
        if (p == null) return null;
        String authorRole = "STUDENT";
        if (p.getThread() != null && p.getThread().getCourse() != null && p.getThread().getCourse().getCreator() != null
                && p.getAuthor() != null && p.getThread().getCourse().getCreator().getId().equals(p.getAuthor().getId())) {
            authorRole = "TEACHER";
        }

        return DiscussionPostResponse.builder()
                .id(p.getId())
                .threadId(p.getThread() != null ? p.getThread().getId() : null)
                .parentId(p.getParent() != null ? p.getParent().getId() : null)
                .authorId(p.getAuthor() != null ? p.getAuthor().getId() : null)
                .authorName(p.getAuthor() != null ? p.getAuthor().getFullName() : null)
                .authorEmail(p.getAuthor() != null ? p.getAuthor().getEmail() : null)
                .authorRole(authorRole)
                .authorAvatarUrl(p.getAuthor() != null ? p.getAuthor().getAvatarUrl() : null)
                .content(p.getContent())
                .isAnswer(p.getIsAnswer())
                .upvoteCount(p.getUpvoteCount())
                .isUpvotedByMe(isUpvotedByMe)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
