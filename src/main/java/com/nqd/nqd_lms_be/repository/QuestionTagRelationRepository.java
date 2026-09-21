package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.QuestionTagRelation;
import com.nqd.nqd_lms_be.entity.QuestionTagRelationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionTagRelationRepository extends JpaRepository<QuestionTagRelation, QuestionTagRelationId> {

    List<QuestionTagRelation> findByQuestionId(UUID questionId);

    @Query("SELECT r.tag.name FROM QuestionTagRelation r WHERE r.questionId = :questionId ORDER BY r.tag.name ASC")
    List<String> findTagNamesByQuestionId(@Param("questionId") UUID questionId);

    @Modifying
    @Query("DELETE FROM QuestionTagRelation r WHERE r.questionId = :questionId")
    void deleteByQuestionId(@Param("questionId") UUID questionId);
}
