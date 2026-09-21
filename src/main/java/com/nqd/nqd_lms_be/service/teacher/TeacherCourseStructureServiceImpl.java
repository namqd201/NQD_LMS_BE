package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.teacher.*;
import com.nqd.nqd_lms_be.entity.Chapter;
import com.nqd.nqd_lms_be.entity.Course;
import com.nqd.nqd_lms_be.entity.Lesson;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.LessonStatus;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class TeacherCourseStructureServiceImpl implements TeacherCourseStructureService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final CourseTeacherRepository courseTeacherRepository;

    @Override
    @Transactional
    public TeacherCourseResponse publishCourse(UUID courseId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseOwnership(course, teacherId);
        course.setStatus(CourseStatus.ACTIVE);
        course = courseRepository.save(course);
        log.info("Teacher {} published course {}", teacherId, courseId);
        return mapToCourseResponse(course);
    }

    @Override
    @Transactional
    public TeacherCourseResponse archiveCourse(UUID courseId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseOwnership(course, teacherId);
        course.setStatus(CourseStatus.ARCHIVED);
        course = courseRepository.save(course);
        log.info("Teacher {} archived course {}", teacherId, courseId);
        return mapToCourseResponse(course);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherCourseDetailResponse getCourseStructure(UUID courseId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseOwnership(course, teacherId);

        List<Chapter> chapters = chapterRepository.findByCourseIdOrderByDisplayOrderAsc(courseId);
        List<TeacherChapterResponse> chapterDtos = chapters.stream()
                .map(this::mapToChapterResponse)
                .collect(Collectors.toList());

        return TeacherCourseDetailResponse.builder()
                .id(course.getId())
                .subjectId(course.getSubject() != null ? course.getSubject().getId() : null)
                .subjectName(course.getSubject() != null ? course.getSubject().getName() : null)
                .name(course.getName())
                .code(course.getCode())
                .description(course.getDescription())
                .gradeLevel(course.getGradeLevel())
                .thumbnailUrl(course.getThumbnailUrl())
                .status(course.getStatus())
                .isPrivate(Boolean.TRUE.equals(course.getIsPrivate()))
                .creatorId(course.getCreator() != null ? course.getCreator().getId() : null)
                .creatorName(course.getCreator() != null ? course.getCreator().getFullName() : null)
                .chapters(chapterDtos)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public TeacherChapterResponse createChapter(UUID courseId, TeacherChapterRequest request, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseOwnership(course, teacherId);

        int order = request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : (int) chapterRepository.countByCourseId(courseId) + 1;

        Chapter chapter = Chapter.builder()
                .course(course)
                .title(request.getTitle())
                .description(request.getDescription())
                .displayOrder(order)
                .price(request.getPrice())
                .isSellable(Boolean.TRUE.equals(request.getIsSellable()))
                .build();

        chapter = chapterRepository.save(chapter);
        log.info("Teacher {} created chapter {} for course {}", teacherId, chapter.getTitle(), courseId);
        return mapToChapterResponse(chapter);
    }

    @Override
    @Transactional
    public TeacherChapterResponse updateChapter(UUID chapterId, TeacherChapterRequest request, UUID teacherId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", chapterId));

        verifyCourseOwnership(chapter.getCourse(), teacherId);

        chapter.setTitle(request.getTitle());
        chapter.setDescription(request.getDescription());
        if (request.getDisplayOrder() != null) {
            chapter.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getPrice() != null) {
            chapter.setPrice(request.getPrice());
        }
        if (request.getIsSellable() != null) {
            chapter.setIsSellable(request.getIsSellable());
        }

        chapter = chapterRepository.save(chapter);
        log.info("Teacher {} updated chapter {}", teacherId, chapterId);
        return mapToChapterResponse(chapter);
    }

    @Override
    @Transactional
    public void reorderChapters(UUID courseId, ReorderItemsRequest request, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseOwnership(course, teacherId);

        int order = 1;
        for (UUID chId : request.getOrderedIds()) {
            Chapter ch = chapterRepository.findById(chId).orElse(null);
            if (ch != null && ch.getCourse().getId().equals(courseId)) {
                ch.setDisplayOrder(order++);
                chapterRepository.save(ch);
            }
        }
        log.info("Teacher {} reordered chapters for course {}", teacherId, courseId);
    }

    @Override
    @Transactional
    public void deleteChapter(UUID chapterId, UUID teacherId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", chapterId));

        verifyCourseOwnership(chapter.getCourse(), teacherId);

        lessonRepository.deleteByChapterId(chapterId);
        chapterRepository.delete(chapter);
        log.info("Teacher {} deleted chapter {}", teacherId, chapterId);
    }

    @Override
    @Transactional
    public TeacherLessonResponse createLesson(UUID chapterId, TeacherLessonRequest request, UUID teacherId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", chapterId));

        verifyCourseOwnership(chapter.getCourse(), teacherId);

        int order = request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : (int) lessonRepository.countByChapterId(chapterId) + 1;

        Lesson lesson = Lesson.builder()
                .chapter(chapter)
                .title(request.getTitle())
                .slug(request.getSlug())
                .summary(request.getSummary())
                .content(request.getContent())
                .videoUrl(request.getVideoUrl())
                .displayOrder(order)
                .estimatedMinutes(request.getEstimatedMinutes() != null ? request.getEstimatedMinutes() : 15)
                .status(request.getStatus() != null ? request.getStatus() : LessonStatus.DRAFT)
                .isPreview(Boolean.TRUE.equals(request.getIsPreview()))
                .price(request.getPrice())
                .isSellable(Boolean.TRUE.equals(request.getIsSellable()))
                .build();

        lesson = lessonRepository.save(lesson);
        log.info("Teacher {} created lesson {} for chapter {}", teacherId, lesson.getTitle(), chapterId);
        return mapToLessonResponse(lesson);
    }

    @Override
    @Transactional
    public TeacherLessonResponse updateLesson(UUID lessonId, TeacherLessonRequest request, UUID teacherId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        verifyCourseOwnership(lesson.getChapter().getCourse(), teacherId);

        lesson.setTitle(request.getTitle());
        lesson.setSlug(request.getSlug());
        lesson.setSummary(request.getSummary());
        lesson.setContent(request.getContent());
        lesson.setVideoUrl(request.getVideoUrl());
        if (request.getDisplayOrder() != null) {
            lesson.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getEstimatedMinutes() != null) {
            lesson.setEstimatedMinutes(request.getEstimatedMinutes());
        }
        if (request.getStatus() != null) {
            lesson.setStatus(request.getStatus());
        }
        if (request.getIsPreview() != null) {
            lesson.setIsPreview(request.getIsPreview());
        }
        if (request.getPrice() != null) {
            lesson.setPrice(request.getPrice());
        }
        if (request.getIsSellable() != null) {
            lesson.setIsSellable(request.getIsSellable());
        }

        lesson = lessonRepository.save(lesson);
        log.info("Teacher {} updated lesson {}", teacherId, lessonId);
        return mapToLessonResponse(lesson);
    }

    @Override
    @Transactional
    public void reorderLessons(UUID chapterId, ReorderItemsRequest request, UUID teacherId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", chapterId));

        verifyCourseOwnership(chapter.getCourse(), teacherId);

        int order = 1;
        for (UUID lId : request.getOrderedIds()) {
            Lesson l = lessonRepository.findById(lId).orElse(null);
            if (l != null && l.getChapter().getId().equals(chapterId)) {
                l.setDisplayOrder(order++);
                lessonRepository.save(l);
            }
        }
        log.info("Teacher {} reordered lessons for chapter {}", teacherId, chapterId);
    }

    @Override
    @Transactional
    public TeacherLessonResponse publishLesson(UUID lessonId, UUID teacherId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        verifyCourseOwnership(lesson.getChapter().getCourse(), teacherId);
        lesson.setStatus(LessonStatus.PUBLISHED);
        lesson = lessonRepository.save(lesson);
        log.info("Teacher {} published lesson {}", teacherId, lessonId);
        return mapToLessonResponse(lesson);
    }

    @Override
    @Transactional
    public TeacherLessonResponse archiveLesson(UUID lessonId, UUID teacherId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        verifyCourseOwnership(lesson.getChapter().getCourse(), teacherId);
        lesson.setStatus(LessonStatus.ARCHIVED);
        lesson = lessonRepository.save(lesson);
        log.info("Teacher {} archived lesson {}", teacherId, lessonId);
        return mapToLessonResponse(lesson);
    }

    @Override
    @Transactional
    public void deleteLesson(UUID lessonId, UUID teacherId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        verifyCourseOwnership(lesson.getChapter().getCourse(), teacherId);
        lessonRepository.delete(lesson);
        log.info("Teacher {} deleted lesson {}", teacherId, lessonId);
    }

    private void verifyCourseOwnership(Course course, UUID teacherId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        boolean isCreator = course.getCreator() != null && course.getCreator().getId().equals(teacherId);
        boolean isAssigned = courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), teacherId);

        if (!isCreator && !isAssigned) {
            throw new ForbiddenOperationException("You do not have permission to manage this course structure");
        }
    }

    private TeacherChapterResponse mapToChapterResponse(Chapter chapter) {
        List<Lesson> lessons = lessonRepository.findByChapterIdOrderByDisplayOrderAsc(chapter.getId());
        List<TeacherLessonResponse> lessonDtos = lessons != null
                ? lessons.stream().map(this::mapToLessonResponse).collect(Collectors.toList())
                : new ArrayList<>();

        return TeacherChapterResponse.builder()
                .id(chapter.getId())
                .courseId(chapter.getCourse().getId())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .displayOrder(chapter.getDisplayOrder())
                .price(chapter.getPrice())
                .isSellable(Boolean.TRUE.equals(chapter.getIsSellable()))
                .lessons(lessonDtos)
                .build();
    }

    private TeacherLessonResponse mapToLessonResponse(Lesson lesson) {
        return TeacherLessonResponse.builder()
                .id(lesson.getId())
                .chapterId(lesson.getChapter().getId())
                .title(lesson.getTitle())
                .slug(lesson.getSlug())
                .summary(lesson.getSummary())
                .content(lesson.getContent())
                .displayOrder(lesson.getDisplayOrder())
                .estimatedMinutes(lesson.getEstimatedMinutes())
                .status(lesson.getStatus())
                .isPreview(Boolean.TRUE.equals(lesson.getIsPreview()))
                .videoUrl(lesson.getVideoUrl())
                .price(lesson.getPrice())
                .isSellable(Boolean.TRUE.equals(lesson.getIsSellable()))
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }

    private TeacherCourseResponse mapToCourseResponse(Course course) {
        return TeacherCourseResponse.builder()
                .id(course.getId())
                .subjectId(course.getSubject() != null ? course.getSubject().getId() : null)
                .subjectName(course.getSubject() != null ? course.getSubject().getName() : null)
                .name(course.getName())
                .code(course.getCode())
                .description(course.getDescription())
                .gradeLevel(course.getGradeLevel())
                .thumbnailUrl(course.getThumbnailUrl())
                .status(course.getStatus())
                .isPrivate(Boolean.TRUE.equals(course.getIsPrivate()))
                .creatorId(course.getCreator() != null ? course.getCreator().getId() : null)
                .creatorName(course.getCreator() != null ? course.getCreator().getFullName() : null)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
