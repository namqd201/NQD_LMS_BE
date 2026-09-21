package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(
    name = "course_reviews",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_review_user", columnNames = {"course_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_course_reviews_course", columnList = "course_id"),
        @Index(name = "idx_course_reviews_user", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class CourseReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
}
