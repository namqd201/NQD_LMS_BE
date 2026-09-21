package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.PostReactionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "post_reactions",
    uniqueConstraints = @UniqueConstraint(name = "uk_post_user_reaction", columnNames = {"post_id", "user_id", "type"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostReaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private DiscussionPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 32)
    private PostReactionType type = PostReactionType.UPVOTE;
}
