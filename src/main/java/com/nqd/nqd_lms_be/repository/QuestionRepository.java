package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question> {
    List<Question> findBySubjectId(UUID subjectId);
    List<Question> findByCourseId(UUID courseId);
    List<Question> findByLessonId(UUID lessonId);
    List<Question> findByCreatorId(UUID creatorId);
    long countByCreatorIdAndIsDeletedFalse(UUID creatorId);
    List<Question> findByCategoryIsNullAndIsDeletedFalse();
    List<Question> findByGradeLevelAndCategoryIsNullAndIsDeletedFalse(String gradeLevel);
}
