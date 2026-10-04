package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.GamificationActionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(
    name = "gamification_xp_logs",
    indexes = {
        @Index(name = "idx_xp_log_user_created", columnList = "user_id, created_at DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class GamificationXpLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "xp_amount", nullable = false)
    private Integer xpAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private GamificationActionType actionType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
