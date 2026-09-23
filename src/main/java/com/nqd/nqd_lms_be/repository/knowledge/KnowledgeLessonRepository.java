package com.nqd.nqd_lms_be.repository.knowledge;

import com.nqd.nqd_lms_be.entity.knowledge.KnowledgeLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeLessonRepository extends JpaRepository<KnowledgeLesson, UUID> {

    List<KnowledgeLesson> findByChapterIdOrderByLessonOrderAsc(UUID chapterId);

    Optional<KnowledgeLesson> findByChapterIdAndLessonOrder(UUID chapterId, Integer lessonOrder);

    long countByChapterId(UUID chapterId);
}
