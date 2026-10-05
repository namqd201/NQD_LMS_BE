package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(
    name = "user_push_subscriptions",
    indexes = {
        @Index(name = "idx_push_sub_user", columnList = "user_id"),
        @Index(name = "idx_push_sub_endpoint", columnList = "endpoint")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class UserPushSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "endpoint", nullable = false, columnDefinition = "TEXT")
    private String endpoint;

    @Column(name = "p256dh", nullable = false, columnDefinition = "TEXT")
    private String p256dh;

    @Column(name = "auth", nullable = false, columnDefinition = "TEXT")
    private String auth;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
}
