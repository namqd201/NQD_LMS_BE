package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Lesson;
import com.nqd.nqd_lms_be.entity.enums.LessonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByChapterIdOrderByDisplayOrderAsc(UUID chapterId);

    Optional<Lesson> findByChapterIdAndDisplayOrder(UUID chapterId, Integer displayOrder);

    List<Lesson> findByChapterIdAndStatusOrderByDisplayOrderAsc(UUID chapterId, LessonStatus status);

    void deleteByChapterId(UUID chapterId);

    long countByChapterId(UUID chapterId);

    @Query("SELECT l FROM Lesson l WHERE l.id = :lessonId AND l.status = :status AND l.chapter.course.status = 'ACTIVE'")
    Optional<Lesson> findPublishedLessonInActiveCourse(@Param("lessonId") UUID lessonId, @Param("status") LessonStatus status);

    @Query("SELECT l FROM Lesson l WHERE l.chapter.course.id = :courseId ORDER BY l.chapter.displayOrder ASC, l.displayOrder ASC")
    List<Lesson> findByChapterCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.chapter.course.id = :courseId")
    long countByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT l FROM Lesson l WHERE l.chapter.course.id = :courseId AND l.status = 'PUBLISHED' ORDER BY l.chapter.displayOrder ASC, l.displayOrder ASC")
    List<Lesson> findPublishedLessonsByCourseId(@Param("courseId") UUID courseId);
}
