package com.nqd.nqd_lms_be.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import com.nqd.nqd_lms_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class ToanLop1CourseSeeder implements CommandLineRunner {

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final ExerciseQuestionRepository exerciseQuestionRepository;
    private final QuestionCategoryRepository questionCategoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking course 'Toán 1' (MATH_GRADE_1) baseline seed data...");

        try {
            ClassPathResource resource = new ClassPathResource("data/toan_lop_1_course_data.json");
            if (!resource.exists()) {
                log.warn("Resource 'data/toan_lop_1_course_data.json' not found. Skipping auto-seed.");
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
                        log.info("Creating subject {} for course seed...", subjectCode);
                        Subject newSubject = Subject.builder()
                                .code(subjectCode)
                                .name(courseNode.get("subjectName").asText())
                                .description("Môn Toán học từ tiểu học đến phổ thông")
                                .status(SubjectStatus.ACTIVE)
                                .build();
                        return subjectRepository.save(newSubject);
                    });

            Optional<Course> existingCourseOpt = courseRepository.findByCode(courseCode);
            Course course;
            if (existingCourseOpt.isEmpty()) {
                log.info("Seeding course '{}' into database...", courseNode.get("name").asText());
                course = Course.builder()
                        .subject(subject)
                        .name(courseNode.get("name").asText())
                        .code(courseCode)
                        .description(courseNode.get("description").asText())
                        .gradeLevel(courseNode.get("gradeLevel").asText())
                        .thumbnailUrl(courseNode.has("thumbnailUrl") ? courseNode.get("thumbnailUrl").asText() : "/images/courses/toan-1.jpg")
                        .status(CourseStatus.ACTIVE)
                        .pricingType(CoursePricingType.FREE)
                        .price(BigDecimal.ZERO)
                        .publishedAt(LocalDateTime.now())
                        .isPrivate(false)
                        .isDisabled(false)
                        .build();
                course = courseRepository.save(course);
            } else {
                course = existingCourseOpt.get();
                // If course already has chapters, skip re-seeding to preserve performance
                long chapterCount = chapterRepository.countByCourseId(course.getId());
                if (chapterCount >= 8) {
                    log.info("Course 'Toán 1' is already fully seeded with {} chapters. Skipping duplicate seed.", chapterCount);
                    return;
                }
            }

            // Find question category for Lớp 1 if available
            QuestionCategory defaultCategory = questionCategoryRepository
                    .findFirstBySubjectIdAndGradeLevelAndVisibilityAndIsDeletedFalse(
                            subject.getId(), "Lớp 1", QuestionCategoryVisibility.PUBLIC
                    )
                    .orElse(null);

            JsonNode chaptersNode = root.get("chapters");
            if (chaptersNode == null || !chaptersNode.isArray()) {
                return;
            }

            int seededChapters = 0;
            int seededLessons = 0;
            int seededQuestions = 0;

            final Course finalCourse = course;
            final Subject finalSubject = subject;

            for (JsonNode chNode : chaptersNode) {
                int chOrder = chNode.get("chapterOrder").asText().isEmpty() ? 1 : chNode.get("chapterOrder").asInt();
                String chTitle = chNode.get("title").asText();
                String chDesc = chNode.has("description") ? chNode.get("description").asText() : "";

                Chapter chapter = chapterRepository.findByCourseIdAndDisplayOrder(finalCourse.getId(), chOrder)
                        .orElseGet(() -> {
                            Chapter newCh = Chapter.builder()
                                    .course(finalCourse)
                                    .title(chTitle)
                                    .description(chDesc)
                                    .displayOrder(chOrder)
                                    .isSellable(false)
                                    .build();
                            return chapterRepository.save(newCh);
                        });
                seededChapters++;

                JsonNode lessonsNode = chNode.get("lessons");
                if (lessonsNode != null && lessonsNode.isArray()) {
                    for (JsonNode lesNode : lessonsNode) {
                        int lesOrder = lesNode.get("displayOrder").asInt();
                        String lesTitle = lesNode.get("title").asText();
                        String lesSlug = lesNode.get("slug").asText();
                        String lesSummary = lesNode.get("summary").asText();
                        String lesTheory = lesNode.get("theory").asText();
                        int estimatedMinutes = lesNode.has("estimatedMinutes") ? lesNode.get("estimatedMinutes").asInt() : 40;

                        Lesson lesson = lessonRepository.findByChapterIdAndDisplayOrder(chapter.getId(), lesOrder)
                                .orElseGet(() -> {
                                    Lesson newLes = Lesson.builder()
                                            .chapter(chapter)
                                            .title(lesTitle)
                                            .slug(lesSlug)
                                            .summary(lesSummary)
                                            .content(lesTheory)
                                            .displayOrder(lesOrder)
                                            .estimatedMinutes(estimatedMinutes)
                                            .status(LessonStatus.PUBLISHED)
                                            .isPreview(true)
                                            .isSellable(false)
                                            .build();
                                    return lessonRepository.save(newLes);
                                });
                        seededLessons++;

                        // Seed Exercise for lesson
                        Exercise exercise = exerciseRepository.findByLessonIdAndIsDeletedFalse(lesson.getId())
                                .stream()
                                .findFirst()
                                .orElseGet(() -> {
                                    Exercise newEx = Exercise.builder()
                                            .lesson(lesson)
                                            .title("Bài tập củng cố: " + lesTitle)
                                            .description("3 câu hỏi trắc nghiệm kiểm tra kiến thức bài: " + lesTitle)
                                            .instructions("Em hãy đọc kỹ câu hỏi và chọn một đáp án đúng nhất nhé!")
                                            .type(ExerciseType.PRACTICE)
                                            .timeLimitMinutes(10)
                                            .passingScore(new BigDecimal("2.00"))
                                            .status(ExerciseStatus.PUBLISHED)
                                            .showExplanationImmediately(true)
                                            .allowRetry(true)
                                            .build();
                                    return exerciseRepository.save(newEx);
                                });

                        // Seed questions if not already attached
                        JsonNode exercisesNode = lesNode.get("exercises");
                        if (exercisesNode != null && exercisesNode.isArray() && exerciseQuestionRepository.countByExerciseId(exercise.getId()) == 0) {
                            int qOrder = 1;
                            for (JsonNode qNode : exercisesNode) {
                                String qContent = qNode.get("question").asText();
                                String qExplanation = qNode.get("explanation").asText();
                                String correctAnswer = qNode.get("correct_answer").asText();

                                Question question = Question.builder()
                                        .subject(finalSubject)
                                        .course(finalCourse)
                                        .lesson(lesson)
                                        .category(defaultCategory)
                                        .gradeLevel("Lớp 1")
                                        .questionType(QuestionType.MULTIPLE_CHOICE)
                                        .difficulty(QuestionDifficulty.EASY)
                                        .content(qContent)
                                        .explanation(qExplanation)
                                        .defaultMarks(new BigDecimal("1.00"))
                                        .source(QuestionSource.MANUAL)
                                        .status(QuestionStatus.APPROVED)
                                        .build();
                                question = questionRepository.save(question);

                                JsonNode optionsNode = qNode.get("options");
                                if (optionsNode != null && optionsNode.isArray()) {
                                    int optIdx = 1;
                                    for (JsonNode optNode : optionsNode) {
                                        String optFullText = optNode.asText();
                                        String optKey = String.valueOf((char) ('A' + optIdx - 1));
                                        boolean isCorrect = optKey.equalsIgnoreCase(correctAnswer);

                                        QuestionOption option = QuestionOption.builder()
                                                .question(question)
                                                .optionKey(optKey)
                                                .optionText(optFullText)
                                                .isCorrect(isCorrect)
                                                .displayOrder(optIdx)
                                                .build();
                                        questionOptionRepository.save(option);
                                        optIdx++;
                                    }
                                }

                                ExerciseQuestion exerciseQuestion = ExerciseQuestion.builder()
                                        .exerciseId(exercise.getId())
                                        .questionId(question.getId())
                                        .exercise(exercise)
                                        .question(question)
                                        .displayOrder(qOrder)
                                        .marks(new BigDecimal("1.00"))
                                        .build();
                                exerciseQuestionRepository.save(exerciseQuestion);

                                qOrder++;
                                seededQuestions++;
                            }
                        }
                    }
                }
            }

            log.info("Successfully seeded course 'Toán 1': {} chapters, {} lessons, and {} questions!",
                    seededChapters, seededLessons, seededQuestions);

        } catch (Exception e) {
            log.error("Error during ToanLop1CourseSeeder execution: {}", e.getMessage(), e);
        }
    }
}
