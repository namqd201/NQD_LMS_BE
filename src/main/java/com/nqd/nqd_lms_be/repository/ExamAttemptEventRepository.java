package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ExamAttemptEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExamAttemptEventRepository extends JpaRepository<ExamAttemptEvent, UUID> {
    List<ExamAttemptEvent> findByAttemptIdOrderByOccurredAtAsc(UUID attemptId);
    long countByAttemptIdAndIsViolationTrue(UUID attemptId);
}
