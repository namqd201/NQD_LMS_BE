package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.QuestionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.nqd.nqd_lms_be.entity.enums.QuestionCategoryVisibility;

@Repository
public interface QuestionCategoryRepository extends JpaRepository<QuestionCategory, UUID> {

    List<QuestionCategory> findBySubjectIdAndGradeLevelAndIsDeletedFalseOrderByDisplayOrderAscCreatedAtAsc(
            UUID subjectId, String gradeLevel
    );

    List<QuestionCategory> findBySubjectIdAndIsDeletedFalseOrderByDisplayOrderAscCreatedAtAsc(
            UUID subjectId
    );

    Optional<QuestionCategory> findFirstBySubjectIdAndGradeLevelAndVisibilityAndIsDeletedFalse(
            UUID subjectId, String gradeLevel, QuestionCategoryVisibility visibility
    );

    boolean existsBySubjectIdAndGradeLevelAndVisibilityAndIsDeletedFalse(
            UUID subjectId, String gradeLevel, QuestionCategoryVisibility visibility
    );

    @Query("SELECT q.category.id, COUNT(q) FROM Question q WHERE q.category IS NOT NULL AND q.isDeleted = false GROUP BY q.category.id")
    List<Object[]> countQuestionsByCategory();

    @Query("SELECT COUNT(q) FROM Question q WHERE q.category.id = :categoryId AND q.isDeleted = false")
    long countQuestionsByCategoryId(@Param("categoryId") UUID categoryId);
}
