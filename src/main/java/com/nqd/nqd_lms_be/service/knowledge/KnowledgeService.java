package com.nqd.nqd_lms_be.service.knowledge;

import com.nqd.nqd_lms_be.dto.knowledge.KnowledgeCurriculumResponse;
import com.nqd.nqd_lms_be.dto.knowledge.KnowledgeLessonResponse;

import java.util.List;
import java.util.UUID;

public interface KnowledgeService {

    KnowledgeCurriculumResponse getCurriculumBySubjectAndGrade(String subjectCode, String gradeLevel);

    KnowledgeCurriculumResponse getCurriculumByCode(String code);

    List<KnowledgeCurriculumResponse> getCurriculumsBySubject(UUID subjectId);

    KnowledgeLessonResponse getLessonDetail(UUID lessonId);
}
