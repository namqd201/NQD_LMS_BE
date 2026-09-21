package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ExerciseQuestion;
import com.nqd.nqd_lms_be.entity.ExerciseQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseQuestionRepository extends JpaRepository<ExerciseQuestion, ExerciseQuestionId> {
    List<ExerciseQuestion> findByExerciseIdOrderByDisplayOrderAsc(UUID exerciseId);
    void deleteByExerciseId(UUID exerciseId);
    void deleteByExerciseIdAndQuestionId(UUID exerciseId, UUID questionId);
    Optional<ExerciseQuestion> findByExerciseIdAndQuestionId(UUID exerciseId, UUID questionId);
    long countByExerciseId(UUID exerciseId);
}
