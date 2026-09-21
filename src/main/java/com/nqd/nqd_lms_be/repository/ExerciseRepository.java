package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Exercise;
import com.nqd.nqd_lms_be.entity.enums.ExerciseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, UUID>, JpaSpecificationExecutor<Exercise> {
    List<Exercise> findByLessonIdAndIsDeletedFalse(UUID lessonId);
    List<Exercise> findByLessonIdAndStatusAndIsDeletedFalse(UUID lessonId, ExerciseStatus status);
    List<Exercise> findByCreatorIdAndIsDeletedFalse(UUID creatorId);
    List<Exercise> findByStatusAndIsDeletedFalse(ExerciseStatus status);
}
