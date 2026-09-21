package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, UUID> {
    List<ExamQuestion> findByExamIdOrderByDisplayOrderAsc(UUID examId);

    @Modifying
    @Query("DELETE FROM ExamQuestion eq WHERE eq.exam.id = :examId")
    void deleteByExamId(@Param("examId") UUID examId);

    long countByExamId(UUID examId);
}
