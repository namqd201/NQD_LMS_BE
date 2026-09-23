package com.nqd.nqd_lms_be.repository.knowledge;

import com.nqd.nqd_lms_be.entity.knowledge.KnowledgeQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeQuestionRepository extends JpaRepository<KnowledgeQuestion, UUID> {

    List<KnowledgeQuestion> findByLessonIdOrderByQuestionOrderAsc(UUID lessonId);

    long countByLessonId(UUID lessonId);
}
