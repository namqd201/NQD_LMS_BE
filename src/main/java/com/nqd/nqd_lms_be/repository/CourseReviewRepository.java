package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, UUID> {

    Page<CourseReview> findByCourseIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID courseId, Pageable pageable);

    List<CourseReview> findByCourseIdAndIsDeletedFalse(UUID courseId);

    Optional<CourseReview> findByCourseIdAndUserIdAndIsDeletedFalse(UUID courseId, UUID userId);

    boolean existsByCourseIdAndUserIdAndIsDeletedFalse(UUID courseId, UUID userId);

    @Query("SELECT AVG(r.rating) FROM CourseReview r WHERE r.course.id = :courseId AND r.isDeleted = false")
    Double getAverageRatingByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT COUNT(r) FROM CourseReview r WHERE r.course.id = :courseId AND r.isDeleted = false")
    long countByCourseIdAndIsDeletedFalse(@Param("courseId") UUID courseId);
}
