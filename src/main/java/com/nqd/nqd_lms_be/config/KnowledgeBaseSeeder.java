package com.nqd.nqd_lms_be.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.entity.Subject;
import com.nqd.nqd_lms_be.entity.enums.SubjectStatus;
import com.nqd.nqd_lms_be.entity.knowledge.KnowledgeChapter;
import com.nqd.nqd_lms_be.entity.knowledge.KnowledgeCurriculum;
import com.nqd.nqd_lms_be.entity.knowledge.KnowledgeLesson;
import com.nqd.nqd_lms_be.entity.knowledge.KnowledgeQuestion;
import com.nqd.nqd_lms_be.repository.CourseRepository;
import com.nqd.nqd_lms_be.repository.SubjectRepository;
import com.nqd.nqd_lms_be.repository.knowledge.KnowledgeChapterRepository;
import com.nqd.nqd_lms_be.repository.knowledge.KnowledgeCurriculumRepository;
import com.nqd.nqd_lms_be.repository.knowledge.KnowledgeLessonRepository;
import com.nqd.nqd_lms_be.repository.knowledge.KnowledgeQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseSeeder implements CommandLineRunner {

    private final SubjectRepository subjectRepository;
    private final KnowledgeCurriculumRepository curriculumRepository;
    private final KnowledgeChapterRepository chapterRepository;
    private final KnowledgeLessonRepository lessonRepository;
    private final KnowledgeQuestionRepository questionRepository;
    private final CourseRepository courseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking Knowledge Base (Admin GDPT Curriculum) baseline data...");

        // Clean up any legacy MATH_GRADE_1 from courses table if it exists
        // (Courses table is strictly for teachers' personal classes)
        try {
            courseRepository.findByCode("MATH_GRADE_1").ifPresent(legacyCourse -> {
                log.info("Removing legacy MATH_GRADE_1 from teacher courses table (ID: {})...", legacyCourse.getId());
                courseRepository.delete(legacyCourse);
            });
        } catch (Exception e) {
            log.warn("Could not clean up legacy course MATH_GRADE_1: {}", e.getMessage());
        }

        try {
            ClassPathResource resource = new ClassPathResource("data/toan_lop_1_course_data.json");
            if (!resource.exists()) {
                log.warn("Resource 'data/toan_lop_1_course_data.json' not found. Skipping Knowledge Base seed.");
                return;
            }

            JsonNode root;
            try (InputStream is = resource.getInputStream()) {
                root = objectMapper.readTree(is);
            }

            JsonNode courseNode = root.get("course");
            String courseCode = courseNode.get("code").asText();
            String subjectCode = courseNode.get("subjectCode").asText();

            Subject subject = subjectRepository.findByCode(subjectCode)
                    .orElseGet(() -> {
                        log.info("Creating subject {} for knowledge base...", subjectCode);
                        Subject newSubject = Subject.builder()
                                .code(subjectCode)
                                .name(courseNode.get("subjectName").asText())
                                .description("Nền tảng tư duy toán học, số học, hình học và giải tích từ Tiểu học đến Đại học")
                                .status(SubjectStatus.ACTIVE)
                                .build();
                        return subjectRepository.save(newSubject);
                    });

            Optional<KnowledgeCurriculum> existingOpt = curriculumRepository.findByCode(courseCode);
            KnowledgeCurriculum curriculum;
            if (existingOpt.isEmpty()) {
                log.info("Creating Knowledge Curriculum '{}' ({}) in knowledge_curriculums table...",
                        courseNode.get("name").asText(), courseCode);

                curriculum = KnowledgeCurriculum.builder()
                        .subject(subject)
                        .code(courseCode)
                        .gradeLevel(courseNode.get("gradeLevel").asText())
                        .title(courseNode.get("name").asText())
                        .description(courseNode.get("description").asText())
                        .educationTier(courseNode.has("educationTier") ? courseNode.get("educationTier").asText() : "Tiểu học")
                        .thumbnailUrl("/images/courses/toan-1.jpg")
                        .status("ACTIVE")
                        .displayOrder(1)
                        .isPublished(true)
                        .build();
                curriculum = curriculumRepository.save(curriculum);
            } else {
                curriculum = existingOpt.get();
                long chapterCount = chapterRepository.countByCurriculumId(curriculum.getId());
                if (chapterCount >= 8) {
                    log.info("Knowledge Curriculum '{}' is already fully seeded with {} chapters. Skipping duplicate seed.",
                            curriculum.getTitle(), chapterCount);
                    return;
                }
            }

            final KnowledgeCurriculum finalCurriculum = curriculum;

            // Seed Chapters, Lessons, and Reinforcement Questions
            JsonNode chaptersNode = root.get("chapters");
            if (chaptersNode != null && chaptersNode.isArray()) {
                int totalLessonsSeeded = 0;
                int totalQuestionsSeeded = 0;

                for (JsonNode chNode : chaptersNode) {
                    int chOrder = chNode.get("chapterOrder").asInt();
                    String chTitle = chNode.get("title").asText();
                    String chDesc = chNode.has("description") ? chNode.get("description").asText() : null;

                    KnowledgeChapter chapter = chapterRepository
                            .findByCurriculumIdAndChapterOrder(finalCurriculum.getId(), chOrder)
                            .orElseGet(() -> {
                                KnowledgeChapter newCh = KnowledgeChapter.builder()
                                        .curriculum(finalCurriculum)
                                        .chapterOrder(chOrder)
                                        .title(chTitle)
                                        .description(chDesc)
                                        .build();
                                return chapterRepository.save(newCh);
                            });

                    JsonNode lessonsNode = chNode.get("lessons");
                    if (lessonsNode != null && lessonsNode.isArray()) {
                        for (JsonNode lesNode : lessonsNode) {
                            int lesOrder = lesNode.get("displayOrder").asInt();
                            String lesTitle = lesNode.get("title").asText();
                            String lesSlug = lesNode.has("slug") ? lesNode.get("slug").asText() : "bai-" + lesOrder;
                            String lesSummary = lesNode.has("summary") ? lesNode.get("summary").asText() : null;
                            String lesTheory = lesNode.has("theory") ? lesNode.get("theory").asText() : "";
                            int estimatedMinutes = lesNode.has("estimatedMinutes") ? lesNode.get("estimatedMinutes").asInt() : 40;

                            KnowledgeLesson lesson = lessonRepository
                                    .findByChapterIdAndLessonOrder(chapter.getId(), lesOrder)
                                    .orElseGet(() -> {
                                        KnowledgeLesson newLes = KnowledgeLesson.builder()
                                                .chapter(chapter)
                                                .lessonOrder(lesOrder)
                                                .title(lesTitle)
                                                .slug(lesSlug)
                                                .summary(lesSummary)
                                                .theoryMarkdown(lesTheory)
                                                .estimatedMinutes(estimatedMinutes)
                                                .status("PUBLISHED")
                                                .build();
                                        return lessonRepository.save(newLes);
                                    });

                            totalLessonsSeeded++;

                            // Seed Questions
                            JsonNode exercisesNode = lesNode.get("exercises");
                            if (exercisesNode != null && exercisesNode.isArray() && questionRepository.countByLessonId(lesson.getId()) == 0) {
                                int qOrder = 1;
                                for (JsonNode exNode : exercisesNode) {
                                    String qText = exNode.get("question").asText();
                                    String correctAnswer = exNode.get("correct_answer").asText();
                                    String explanation = exNode.has("explanation") ? exNode.get("explanation").asText() : null;

                                    List<String> optionsList = new ArrayList<>();
                                    JsonNode optionsNode = exNode.get("options");
                                    if (optionsNode != null && optionsNode.isArray()) {
                                        for (JsonNode opt : optionsNode) {
                                            optionsList.add(opt.asText());
                                        }
                                    }

                                    KnowledgeQuestion question = KnowledgeQuestion.builder()
                                            .lesson(lesson)
                                            .questionOrder(qOrder++)
                                            .questionText(qText)
                                            .optionsJson(objectMapper.writeValueAsString(optionsList))
                                            .correctAnswer(correctAnswer)
                                            .explanation(explanation)
                                            .build();

                                    questionRepository.save(question);
                                    totalQuestionsSeeded++;
                                }
                            }
                        }
                    }
                }

                log.info("Knowledge Base seed completed successfully: {} chapters, {} lessons, {} reinforcement questions.",
                        chaptersNode.size(), totalLessonsSeeded, totalQuestionsSeeded);
            }

        } catch (Exception e) {
            log.error("Error during KnowledgeBaseSeeder execution: {}", e.getMessage(), e);
        }
    }
}
