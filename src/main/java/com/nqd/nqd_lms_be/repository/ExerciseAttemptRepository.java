package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ExerciseAttempt;
import com.nqd.nqd_lms_be.entity.enums.ExerciseAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttempt, UUID> {
    List<ExerciseAttempt> findByExerciseIdAndStudentIdOrderByAttemptNumberDesc(UUID exerciseId, UUID studentId);
    List<ExerciseAttempt> findByExerciseIdAndStudentIdOrderByAttemptNumberAsc(UUID exerciseId, UUID studentId);
    Optional<ExerciseAttempt> findFirstByExerciseIdAndStudentIdAndStatusOrderByStartedAtDesc(UUID exerciseId, UUID studentId, ExerciseAttemptStatus status);
    long countByExerciseIdAndStudentId(UUID exerciseId, UUID studentId);
    List<ExerciseAttempt> findByStudentIdOrderByStartedAtDesc(UUID studentId);
}
