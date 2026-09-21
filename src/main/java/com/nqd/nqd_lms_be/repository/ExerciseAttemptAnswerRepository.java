package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ExerciseAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseAttemptAnswerRepository extends JpaRepository<ExerciseAttemptAnswer, UUID> {
    List<ExerciseAttemptAnswer> findByAttemptId(UUID attemptId);
    Optional<ExerciseAttemptAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
    void deleteByAttemptId(UUID attemptId);
}
