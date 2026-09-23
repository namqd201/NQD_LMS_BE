package com.nqd.nqd_lms_be.service.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.dto.knowledge.KnowledgeChapterResponse;
import com.nqd.nqd_lms_be.dto.knowledge.KnowledgeCurriculumResponse;
import com.nqd.nqd_lms_be.dto.knowledge.KnowledgeLessonResponse;
import com.nqd.nqd_lms_be.dto.knowledge.KnowledgeQuestionResponse;
import com.nqd.nqd_lms_be.entity.knowledge.KnowledgeChapter;
import com.nqd.nqd_lms_be.entity.knowledge.KnowledgeCurriculum;
import com.nqd.nqd_lms_be.entity.knowledge.KnowledgeLesson;
import com.nqd.nqd_lms_be.entity.knowledge.KnowledgeQuestion;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.repository.knowledge.KnowledgeChapterRepository;
import com.nqd.nqd_lms_be.repository.knowledge.KnowledgeCurriculumRepository;
import com.nqd.nqd_lms_be.repository.knowledge.KnowledgeLessonRepository;
import com.nqd.nqd_lms_be.repository.knowledge.KnowledgeQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeCurriculumRepository curriculumRepository;
    private final KnowledgeChapterRepository chapterRepository;
    private final KnowledgeLessonRepository lessonRepository;
    private final KnowledgeQuestionRepository questionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public KnowledgeCurriculumResponse getCurriculumBySubjectAndGrade(String subjectCode, String gradeLevel) {
        KnowledgeCurriculum curriculum = curriculumRepository
                .findBySubjectNameOrCodeAndGradeLevel(subjectCode, gradeLevel)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeCurriculum", subjectCode + " - " + gradeLevel));

        return mapToDetailResponse(curriculum);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeCurriculumResponse getCurriculumByCode(String code) {
        KnowledgeCurriculum curriculum = curriculumRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeCurriculum", code));

        return mapToDetailResponse(curriculum);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeCurriculumResponse> getCurriculumsBySubject(UUID subjectId) {
        return curriculumRepository.findBySubjectIdAndIsPublishedTrueOrderByDisplayOrderAsc(subjectId)
                .stream()
                .map(this::mapToDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeLessonResponse getLessonDetail(UUID lessonId) {
        KnowledgeLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeLesson", lessonId));

        return mapToLessonResponse(lesson);
    }

    private KnowledgeCurriculumResponse mapToDetailResponse(KnowledgeCurriculum curriculum) {
        List<KnowledgeChapter> chapters = chapterRepository.findByCurriculumIdOrderByChapterOrderAsc(curriculum.getId());

        List<KnowledgeChapterResponse> chapterResponses = chapters.stream().map(ch -> {
            List<KnowledgeLesson> lessons = lessonRepository.findByChapterIdOrderByLessonOrderAsc(ch.getId());
            List<KnowledgeLessonResponse> lessonResponses = lessons.stream()
                    .map(this::mapToLessonResponse)
                    .collect(Collectors.toList());

            return KnowledgeChapterResponse.builder()
                    .id(ch.getId())
                    .chapterOrder(ch.getChapterOrder())
                    .title(ch.getTitle())
                    .description(ch.getDescription())
                    .lessons(lessonResponses)
                    .build();
        }).collect(Collectors.toList());

        int totalLessons = chapterResponses.stream().mapToInt(c -> c.getLessons() != null ? c.getLessons().size() : 0).sum();

        return KnowledgeCurriculumResponse.builder()
                .id(curriculum.getId())
                .code(curriculum.getCode())
                .title(curriculum.getTitle())
                .description(curriculum.getDescription())
                .gradeLevel(curriculum.getGradeLevel())
                .educationTier(curriculum.getEducationTier())
                .subjectCode(curriculum.getSubject() != null ? curriculum.getSubject().getCode() : null)
                .subjectName(curriculum.getSubject() != null ? curriculum.getSubject().getName() : null)
                .thumbnailUrl(curriculum.getThumbnailUrl())
                .totalChapters(chapterResponses.size())
                .totalLessons(totalLessons)
                .chapters(chapterResponses)
                .build();
    }

    private KnowledgeLessonResponse mapToLessonResponse(KnowledgeLesson lesson) {
        List<KnowledgeQuestion> questions = questionRepository.findByLessonIdOrderByQuestionOrderAsc(lesson.getId());

        List<KnowledgeQuestionResponse> questionResponses = questions.stream().map(q -> {
            List<String> options = parseOptions(q.getOptionsJson());
            return KnowledgeQuestionResponse.builder()
                    .id(q.getId())
                    .questionOrder(q.getQuestionOrder())
                    .questionText(q.getQuestionText())
                    .options(options)
                    .correctAnswer(q.getCorrectAnswer())
                    .explanation(q.getExplanation())
                    .build();
        }).collect(Collectors.toList());

        return KnowledgeLessonResponse.builder()
                .id(lesson.getId())
                .lessonOrder(lesson.getLessonOrder())
                .title(lesson.getTitle())
                .slug(lesson.getSlug())
                .summary(lesson.getSummary())
                .theoryMarkdown(lesson.getTheoryMarkdown())
                .estimatedMinutes(lesson.getEstimatedMinutes())
                .status(lesson.getStatus())
                .questions(questionResponses)
                .build();
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Could not parse optionsJson as List<String>: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
