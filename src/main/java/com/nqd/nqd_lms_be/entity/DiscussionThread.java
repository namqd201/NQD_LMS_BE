package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.DiscussionThreadStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "discussion_threads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscussionThread extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 32)
    private DiscussionThreadStatus status = DiscussionThreadStatus.OPEN;

    @Builder.Default
    @Column(name = "post_count", nullable = false)
    private Integer postCount = 0;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;
}
