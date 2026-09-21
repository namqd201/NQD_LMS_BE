package com.nqd.nqd_lms_be.ai;

import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.LessonStatus;
import com.nqd.nqd_lms_be.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Safe, read-only data access gateway for AI Tutor.
 * All queries enforce role-based access control — AI never touches repositories directly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiTutorToolService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final LessonResourceRepository lessonResourceRepository;
    private final QuestionRepository questionRepository;

    // ========== DTOs for safe data transfer to AI context ==========

    @Data
    @Builder
    public static class CourseInfo {
        private UUID id;
        private String name;
        private String code;
        private String description;
        private String gradeLevel;
        private String subjectName;
    }

    @Data
    @Builder
    public static class ChapterInfo {
        private UUID id;
        private String title;
        private String description;
        private int displayOrder;
    }

    @Data
    @Builder
    public static class LessonInfo {
        private UUID id;
        private String title;
        private String summary;
        private String content;
        private int displayOrder;
        private String chapterTitle;
    }

    @Data
    @Builder
    public static class QuestionInfo {
        private UUID id;
        private String content;
        private String questionType;
        private String difficulty;
        private String explanation;
    }

    // ========== Safe Query Methods ==========

    /**
     * Search courses by keyword with role-based filtering.
     */
    @Transactional(readOnly = true)
    public List<CourseInfo> searchCourses(String keyword, UUID userId, String userRole) {
        log.info("AI Tool: searchCourses(keyword='{}', userId={}, role={})", keyword, userId, userRole);

        List<Course> candidates;
        String roleUpper = userRole != null ? userRole.toUpperCase() : "STUDENT";

        switch (roleUpper) {
            case "ADMIN" -> candidates = courseRepository.findAll();
            case "TEACHER" -> candidates = courseRepository.findCoursesByTeacherId(userId);
            default -> {
                // STUDENT: only enrolled courses with ACTIVE status
                List<CourseEnrollment> enrollments = enrollmentRepository.findByStudentId(userId);
                candidates = enrollments.stream()
                        .map(CourseEnrollment::getCourse)
                        .filter(Objects::nonNull)
                        .filter(c -> c.getStatus() == CourseStatus.ACTIVE)
                        .collect(Collectors.toList());
            }
        }

        // Filter by keyword (case-insensitive, match name or code)
        if (keyword != null && !keyword.isBlank()) {
            String lowerKw = keyword.toLowerCase(Locale.ROOT).trim();
            // Split keyword into tokens for flexible matching
            String[] tokens = lowerKw.split("\\s+");

            candidates = candidates.stream()
                    .filter(c -> {
                        String searchTarget = ((c.getName() != null ? c.getName() : "") + " " +
                                (c.getCode() != null ? c.getCode() : "") + " " +
                                (c.getDescription() != null ? c.getDescription() : "")).toLowerCase(Locale.ROOT);
                        // Match if all tokens are found in the search target
                        return Arrays.stream(tokens).allMatch(searchTarget::contains);
                    })
                    .collect(Collectors.toList());
        }

        return candidates.stream()
                .limit(10)
                .map(c -> CourseInfo.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .code(c.getCode())
                        .description(c.getDescription())
                        .gradeLevel(c.getGradeLevel())
                        .subjectName(c.getSubject() != null ? c.getSubject().getName() : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get chapters of a course with access check.
     */
    @Transactional(readOnly = true)
    public List<ChapterInfo> getChaptersByCourse(UUID courseId, UUID userId, String userRole) {
        log.info("AI Tool: getChaptersByCourse(courseId={}, userId={}, role={})", courseId, userId, userRole);

        if (!hasAccessToCourse(courseId, userId, userRole)) {
            log.warn("AI Tool: Access denied for user {} to course {}", userId, courseId);
            return Collections.emptyList();
        }

        return chapterRepository.findByCourseIdOrderByDisplayOrderAsc(courseId).stream()
                .map(ch -> ChapterInfo.builder()
                        .id(ch.getId())
                        .title(ch.getTitle())
                        .description(ch.getDescription())
                        .displayOrder(ch.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get lessons by chapter with access check.
     * Students only see PUBLISHED lessons.
     */
    @Transactional(readOnly = true)
    public List<LessonInfo> getLessonsByChapter(UUID chapterId, UUID userId, String userRole) {
        log.info("AI Tool: getLessonsByChapter(chapterId={}, userId={}, role={})", chapterId, userId, userRole);

        String roleUpper = userRole != null ? userRole.toUpperCase() : "STUDENT";
        List<Lesson> lessons;

        if ("STUDENT".equals(roleUpper)) {
            lessons = lessonRepository.findByChapterIdAndStatusOrderByDisplayOrderAsc(chapterId, LessonStatus.PUBLISHED);
        } else {
            lessons = lessonRepository.findByChapterIdOrderByDisplayOrderAsc(chapterId);
        }

        // Verify course access through chapter
        if (!lessons.isEmpty()) {
            Lesson first = lessons.get(0);
            if (first.getChapter() != null && first.getChapter().getCourse() != null) {
                UUID courseId = first.getChapter().getCourse().getId();
                if (!hasAccessToCourse(courseId, userId, userRole)) {
                    log.warn("AI Tool: Access denied for user {} to chapter {}", userId, chapterId);
                    return Collections.emptyList();
                }
            }
        }

        return lessons.stream()
                .map(l -> LessonInfo.builder()
                        .id(l.getId())
                        .title(l.getTitle())
                        .summary(l.getSummary())
                        .content(truncateContent(l.getContent(), 2000))
                        .displayOrder(l.getDisplayOrder())
                        .chapterTitle(l.getChapter() != null ? l.getChapter().getTitle() : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get full lesson content for a specific lesson with access check.
     */
    @Transactional(readOnly = true)
    public LessonInfo getLessonContent(UUID lessonId, UUID userId, String userRole) {
        log.info("AI Tool: getLessonContent(lessonId={}, userId={}, role={})", lessonId, userId, userRole);

        Optional<Lesson> opt = lessonRepository.findById(lessonId);
        if (opt.isEmpty()) return null;

        Lesson lesson = opt.get();
        String roleUpper = userRole != null ? userRole.toUpperCase() : "STUDENT";

        // Students can only see PUBLISHED lessons
        if ("STUDENT".equals(roleUpper) && lesson.getStatus() != LessonStatus.PUBLISHED) {
            log.warn("AI Tool: Student {} tried to access non-published lesson {}", userId, lessonId);
            return null;
        }

        // Check course access
        if (lesson.getChapter() != null && lesson.getChapter().getCourse() != null) {
            UUID courseId = lesson.getChapter().getCourse().getId();
            if (!hasAccessToCourse(courseId, userId, userRole)) {
                return null;
            }
        }

        return LessonInfo.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .summary(lesson.getSummary())
                .content(truncateContent(lesson.getContent(), 4000))
                .displayOrder(lesson.getDisplayOrder())
                .chapterTitle(lesson.getChapter() != null ? lesson.getChapter().getTitle() : null)
                .build();
    }

    /**
     * Get existing questions for a course/lesson. Only for TEACHER and ADMIN.
     */
    @Transactional(readOnly = true)
    public List<QuestionInfo> getExistingQuestions(UUID courseId, UUID lessonId, UUID userId, String userRole) {
        String roleUpper = userRole != null ? userRole.toUpperCase() : "STUDENT";

        // Students cannot access question bank
        if ("STUDENT".equals(roleUpper)) {
            log.warn("AI Tool: Student {} tried to access question bank", userId);
            return Collections.emptyList();
        }

        // Teacher must own the course
        if ("TEACHER".equals(roleUpper) && courseId != null && !hasAccessToCourse(courseId, userId, userRole)) {
            return Collections.emptyList();
        }

        List<Question> questions;
        if (lessonId != null) {
            questions = questionRepository.findByLessonId(lessonId);
        } else if (courseId != null) {
            questions = questionRepository.findByCourseId(courseId);
        } else {
            return Collections.emptyList();
        }

        return questions.stream()
                .limit(20)
                .map(q -> QuestionInfo.builder()
                        .id(q.getId())
                        .content(q.getContent())
                        .questionType(q.getQuestionType() != null ? q.getQuestionType().name() : null)
                        .difficulty(q.getDifficulty() != null ? q.getDifficulty().name() : null)
                        .explanation(q.getExplanation())
                        .build())
                .collect(Collectors.toList());
    }

    // ========== Access Control Helpers ==========

    private boolean hasAccessToCourse(UUID courseId, UUID userId, String userRole) {
        if (courseId == null || userId == null) return false;
        String roleUpper = userRole != null ? userRole.toUpperCase() : "STUDENT";

        return switch (roleUpper) {
            case "ADMIN" -> true;
            case "TEACHER" -> courseRepository.isTeacherOwnerOrAssigned(courseId, userId);
            default -> enrollmentRepository.existsByCourseIdAndStudentId(courseId, userId);
        };
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return null;
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "... (nội dung đã được rút gọn)";
    }

    // ========== Format Fetched Data as Context for AI Prompt ==========

    /**
     * Format fetched course + chapter + lesson data into a human-readable context string
     * to be injected into the AI prompt.
     */
    public String formatCourseContext(List<CourseInfo> courses, List<ChapterInfo> chapters, List<LessonInfo> lessons) {
        StringBuilder sb = new StringBuilder();

        if (courses != null && !courses.isEmpty()) {
            sb.append("\n=== THÔNG TIN KHÓA HỌC TÌM ĐƯỢC ===\n");
            for (CourseInfo c : courses) {
                sb.append(String.format("- Khóa học: \"%s\" (Mã: %s)", c.getName(), c.getCode()));
                if (c.getSubjectName() != null) sb.append(" | Môn: ").append(c.getSubjectName());
                if (c.getGradeLevel() != null) sb.append(" | Khối: ").append(c.getGradeLevel());
                sb.append("\n");
                if (c.getDescription() != null && !c.getDescription().isBlank()) {
                    sb.append("  Mô tả: ").append(c.getDescription()).append("\n");
                }
            }
        }

        if (chapters != null && !chapters.isEmpty()) {
            sb.append("\n=== DANH SÁCH CHƯƠNG ===\n");
            for (ChapterInfo ch : chapters) {
                sb.append(String.format("- Chương %d: \"%s\"", ch.getDisplayOrder(), ch.getTitle()));
                if (ch.getDescription() != null) sb.append(" — ").append(ch.getDescription());
                sb.append("\n");
            }
        }

        if (lessons != null && !lessons.isEmpty()) {
            sb.append("\n=== NỘI DUNG BÀI GIẢNG ===\n");
            for (LessonInfo l : lessons) {
                sb.append(String.format("--- Bài %d: \"%s\"", l.getDisplayOrder(), l.getTitle()));
                if (l.getChapterTitle() != null) sb.append(" (Chương: ").append(l.getChapterTitle()).append(")");
                sb.append(" ---\n");
                if (l.getSummary() != null && !l.getSummary().isBlank()) {
                    sb.append("Tóm tắt: ").append(l.getSummary()).append("\n");
                }
                if (l.getContent() != null && !l.getContent().isBlank()) {
                    sb.append("Nội dung:\n").append(l.getContent()).append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
