package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.DiscussionThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscussionThreadRepository extends JpaRepository<DiscussionThread, UUID>, JpaSpecificationExecutor<DiscussionThread> {

    @Query("SELECT t FROM DiscussionThread t LEFT JOIN FETCH t.author LEFT JOIN FETCH t.lesson WHERE t.id = :id AND t.isDeleted = false")
    Optional<DiscussionThread> findByIdWithDetails(@Param("id") UUID id);

    Page<DiscussionThread> findByCourseIdAndIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(UUID courseId, Pageable pageable);

    Page<DiscussionThread> findByCourseIdAndLessonIdAndIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(UUID courseId, UUID lessonId, Pageable pageable);

    long countByCourseIdAndIsDeletedFalse(UUID courseId);

    @Modifying
    @Query("UPDATE DiscussionThread t SET t.viewCount = t.viewCount + 1 WHERE t.id = :id")
    void incrementViewCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE DiscussionThread t SET t.postCount = t.postCount + 1 WHERE t.id = :id")
    void incrementPostCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE DiscussionThread t SET t.postCount = CASE WHEN t.postCount > 0 THEN t.postCount - 1 ELSE 0 END WHERE t.id = :id")
    void decrementPostCount(@Param("id") UUID id);
}
