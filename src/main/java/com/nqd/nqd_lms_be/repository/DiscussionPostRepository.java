package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.DiscussionPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, UUID> {

    @Query("SELECT p FROM DiscussionPost p LEFT JOIN FETCH p.author WHERE p.thread.id = :threadId AND p.isDeleted = false ORDER BY p.isAnswer DESC, p.upvoteCount DESC, p.createdAt ASC")
    List<DiscussionPost> findByThreadIdOrderByHierarchy(@Param("threadId") UUID threadId);

    @Query(value = "SELECT p FROM DiscussionPost p LEFT JOIN FETCH p.author WHERE p.thread.id = :threadId AND p.isDeleted = false ORDER BY p.isAnswer DESC, p.upvoteCount DESC, p.createdAt ASC",
           countQuery = "SELECT COUNT(p) FROM DiscussionPost p WHERE p.thread.id = :threadId AND p.isDeleted = false")
    Page<DiscussionPost> findByThreadId(@Param("threadId") UUID threadId, Pageable pageable);

    long countByThreadIdAndIsDeletedFalse(UUID threadId);

    @Modifying
    @Query("UPDATE DiscussionPost p SET p.upvoteCount = p.upvoteCount + 1 WHERE p.id = :id")
    void incrementUpvoteCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE DiscussionPost p SET p.upvoteCount = CASE WHEN p.upvoteCount > 0 THEN p.upvoteCount - 1 ELSE 0 END WHERE p.id = :id")
    void decrementUpvoteCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE DiscussionPost p SET p.isAnswer = false WHERE p.thread.id = :threadId AND p.id <> :postId")
    void resetOtherAnswersInThread(@Param("threadId") UUID threadId, @Param("postId") UUID postId);
}
