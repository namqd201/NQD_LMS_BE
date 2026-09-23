package com.nqd.nqd_lms_be.repository.knowledge;

import com.nqd.nqd_lms_be.entity.knowledge.KnowledgeChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeChapterRepository extends JpaRepository<KnowledgeChapter, UUID> {

    List<KnowledgeChapter> findByCurriculumIdOrderByChapterOrderAsc(UUID curriculumId);

    Optional<KnowledgeChapter> findByCurriculumIdAndChapterOrder(UUID curriculumId, Integer chapterOrder);

    long countByCurriculumId(UUID curriculumId);
}
