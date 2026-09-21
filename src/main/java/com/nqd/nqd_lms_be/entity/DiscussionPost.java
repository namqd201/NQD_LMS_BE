package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "discussion_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscussionPost extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private DiscussionThread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private DiscussionPost parent;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    @Column(name = "is_answer", nullable = false)
    private Boolean isAnswer = false;

    @Builder.Default
    @Column(name = "upvote_count", nullable = false)
    private Integer upvoteCount = 0;
}
