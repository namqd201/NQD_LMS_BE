package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.AiGeneratedQuestion;
import com.nqd.nqd_lms_be.entity.enums.AiReviewStatus;
import com.nqd.nqd_lms_be.entity.enums.AiValidationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiGeneratedQuestionRepository extends JpaRepository<AiGeneratedQuestion, UUID> {

    @EntityGraph(attributePaths = {"options"})
    List<AiGeneratedQuestion> findByJobIdOrderByDisplayOrderAsc(UUID jobId);
    List<AiGeneratedQuestion> findByJobIdAndReviewStatus(UUID jobId, AiReviewStatus reviewStatus);
    List<AiGeneratedQuestion> findByJobIdAndValidationStatus(UUID jobId, AiValidationStatus validationStatus);
    long countByJobIdAndReviewStatus(UUID jobId, AiReviewStatus reviewStatus);
}
