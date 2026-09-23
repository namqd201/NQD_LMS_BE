package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.PostReactionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "thread_reactions",
    uniqueConstraints = @UniqueConstraint(name = "uk_thread_user_reaction", columnNames = {"thread_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadReaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private DiscussionThread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PostReactionType type;
}
