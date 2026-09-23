package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ThreadReaction;
import com.nqd.nqd_lms_be.entity.enums.PostReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ThreadReactionRepository extends JpaRepository<ThreadReaction, UUID> {

    Optional<ThreadReaction> findByThreadIdAndUserId(UUID threadId, UUID userId);

    List<ThreadReaction> findByThreadId(UUID threadId);

    long countByThreadId(UUID threadId);

    boolean existsByThreadIdAndUserId(UUID threadId, UUID userId);

    void deleteByThreadIdAndUserId(UUID threadId, UUID userId);
}
