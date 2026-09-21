package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, UUID> {
    List<ExamAnswer> findByAttemptId(UUID attemptId);
    Optional<ExamAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
}
