package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.AiGenerationJob;
import com.nqd.nqd_lms_be.entity.enums.AiJobStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiGenerationJobRepository extends JpaRepository<AiGenerationJob, UUID> {

    @Override
    @EntityGraph(attributePaths = {"subject", "course", "lesson", "creator", "createdExam", "category"})
    Optional<AiGenerationJob> findById(UUID id);

    @EntityGraph(attributePaths = {"subject", "course", "lesson", "creator", "createdExam", "category"})
    List<AiGenerationJob> findByCreatorIdOrderByCreatedAtDesc(UUID creatorId);

    @EntityGraph(attributePaths = {"subject", "course", "lesson", "creator", "createdExam", "category"})
    List<AiGenerationJob> findByStatusOrderByCreatedAtDesc(AiJobStatus status);
}
