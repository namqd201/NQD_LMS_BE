package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.PostReaction;
import com.nqd.nqd_lms_be.entity.enums.PostReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, UUID> {

    Optional<PostReaction> findByPostIdAndUserIdAndType(UUID postId, UUID userId, PostReactionType type);

    boolean existsByPostIdAndUserIdAndType(UUID postId, UUID userId, PostReactionType type);

    void deleteByPostIdAndUserIdAndType(UUID postId, UUID userId, PostReactionType type);
}
