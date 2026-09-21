package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.AiTutorMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiTutorMessageRepository extends JpaRepository<AiTutorMessage, UUID> {

    List<AiTutorMessage> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
