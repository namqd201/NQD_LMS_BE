package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.dto.teacher.*;

import java.util.UUID;

public interface TeacherCourseStructureService {
    TeacherCourseResponse publishCourse(UUID courseId, UUID teacherId);
    TeacherCourseResponse archiveCourse(UUID courseId, UUID teacherId);

    TeacherCourseDetailResponse getCourseStructure(UUID courseId, UUID teacherId);

    TeacherChapterResponse createChapter(UUID courseId, TeacherChapterRequest request, UUID teacherId);
    TeacherChapterResponse updateChapter(UUID chapterId, TeacherChapterRequest request, UUID teacherId);
    void reorderChapters(UUID courseId, ReorderItemsRequest request, UUID teacherId);
    void deleteChapter(UUID chapterId, UUID teacherId);

    TeacherLessonResponse createLesson(UUID chapterId, TeacherLessonRequest request, UUID teacherId);
    TeacherLessonResponse updateLesson(UUID lessonId, TeacherLessonRequest request, UUID teacherId);
    void reorderLessons(UUID chapterId, ReorderItemsRequest request, UUID teacherId);
    TeacherLessonResponse publishLesson(UUID lessonId, UUID teacherId);
    TeacherLessonResponse archiveLesson(UUID lessonId, UUID teacherId);
    void deleteLesson(UUID lessonId, UUID teacherId);
}
