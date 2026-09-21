package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, UUID> {
    List<QuestionOption> findByQuestionIdOrderByDisplayOrderAsc(UUID questionId);

    @Modifying
    @Query("DELETE FROM QuestionOption qo WHERE qo.question.id = :questionId")
    void deleteByQuestionId(@Param("questionId") UUID questionId);
}
