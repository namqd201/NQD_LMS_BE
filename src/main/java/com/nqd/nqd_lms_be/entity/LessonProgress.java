package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "lesson_progress",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "lesson_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class LessonProgress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private LessonProgressStatus status = LessonProgressStatus.IN_PROGRESS;

    @Column(name = "progress_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal progressPercent = BigDecimal.ZERO;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_accessed_at", nullable = false)
    private LocalDateTime lastAccessedAt;

    @Column(name = "is_unlocked_by_admin")
    @Builder.Default
    private Boolean isUnlockedByAdmin = false;

    @Column(name = "video_watched")
    @Builder.Default
    private Boolean videoWatched = false;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (lastAccessedAt == null) {
            lastAccessedAt = LocalDateTime.now();
        }
    }
}
