package com.nqd.nqd_lms_be.repository.knowledge;

import com.nqd.nqd_lms_be.entity.knowledge.KnowledgeCurriculum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeCurriculumRepository extends JpaRepository<KnowledgeCurriculum, UUID> {

    Optional<KnowledgeCurriculum> findByCode(String code);

    @Query("SELECT c FROM KnowledgeCurriculum c WHERE LOWER(c.subject.code) = LOWER(:subjectCode) AND c.gradeLevel = :gradeLevel AND c.isPublished = true")
    Optional<KnowledgeCurriculum> findBySubjectCodeAndGradeLevel(@Param("subjectCode") String subjectCode, @Param("gradeLevel") String gradeLevel);

    @Query("SELECT c FROM KnowledgeCurriculum c WHERE (LOWER(c.subject.name) = LOWER(:subjectName) OR LOWER(c.subject.code) = LOWER(:subjectName)) AND c.gradeLevel = :gradeLevel AND c.isPublished = true")
    Optional<KnowledgeCurriculum> findBySubjectNameOrCodeAndGradeLevel(@Param("subjectName") String subjectName, @Param("gradeLevel") String gradeLevel);

    List<KnowledgeCurriculum> findBySubjectIdAndIsPublishedTrueOrderByDisplayOrderAsc(UUID subjectId);
}
