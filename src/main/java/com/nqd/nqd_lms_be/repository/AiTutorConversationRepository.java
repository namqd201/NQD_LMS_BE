package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.AiTutorConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiTutorConversationRepository extends JpaRepository<AiTutorConversation, UUID> {

    List<AiTutorConversation> findAllByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(UUID userId);

    Optional<AiTutorConversation> findByIdAndUserIdAndIsActiveTrue(UUID id, UUID userId);
}
