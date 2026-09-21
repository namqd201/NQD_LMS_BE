package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    Optional<LessonProgress> findByStudentIdAndLessonId(UUID studentId, UUID lessonId);

    List<LessonProgress> findByStudentId(UUID studentId);

    List<LessonProgress> findByLessonId(UUID lessonId);

    long countByStudentIdAndStatus(UUID studentId, com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus status);

    long countByLessonIdAndStatus(UUID lessonId, com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus status);

    @Query("SELECT lp FROM LessonProgress lp JOIN lp.lesson l JOIN l.chapter ch WHERE ch.course.id = :courseId")
    List<LessonProgress> findByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT lp FROM LessonProgress lp JOIN lp.lesson l JOIN l.chapter ch WHERE lp.student.id = :studentId AND ch.course.id = :courseId")
    List<LessonProgress> findByStudentIdAndCourseId(@Param("studentId") UUID studentId, @Param("courseId") UUID courseId);

    @Query("SELECT l.id, l.title, ch.title, l.displayOrder, " +
           "SUM(CASE WHEN lp.status = com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus.IN_PROGRESS THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN lp.status = com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus.COMPLETED THEN 1 ELSE 0 END) " +
           "FROM LessonProgress lp JOIN lp.lesson l JOIN l.chapter ch " +
           "WHERE ch.course.id = :courseId " +
           "GROUP BY l.id, l.title, ch.title, l.displayOrder " +
           "ORDER BY SUM(CASE WHEN lp.status = com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus.IN_PROGRESS THEN 1 ELSE 0 END) DESC")
    List<Object[]> findLessonDropOffStatsByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT CAST(lp.completedAt AS date), COUNT(lp) " +
           "FROM LessonProgress lp JOIN lp.lesson l JOIN l.chapter ch " +
           "WHERE ch.course.id = :courseId AND lp.completedAt IS NOT NULL AND lp.completedAt >= :startDate " +
           "GROUP BY CAST(lp.completedAt AS date) " +
           "ORDER BY CAST(lp.completedAt AS date) ASC")
    List<Object[]> findDailyLessonCompletionsByCourseId(@Param("courseId") UUID courseId, @Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT COALESCE(SUM(l.estimatedMinutes), 0) " +
           "FROM LessonProgress lp JOIN lp.lesson l " +
           "WHERE lp.student.id = :studentId AND lp.status = com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus.COMPLETED")
    Long sumCompletedEstimatedMinutesByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT DISTINCT CAST(COALESCE(lp.lastAccessedAt, lp.completedAt, lp.startedAt) AS date) " +
           "FROM LessonProgress lp " +
           "WHERE lp.student.id = :studentId " +
           "ORDER BY CAST(COALESCE(lp.lastAccessedAt, lp.completedAt, lp.startedAt) AS date) DESC")
    List<java.sql.Date> findDistinctActivityDatesByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT CAST(COALESCE(lp.lastAccessedAt, lp.completedAt, lp.startedAt) AS date), COUNT(lp) " +
           "FROM LessonProgress lp " +
           "WHERE lp.student.id = :studentId AND COALESCE(lp.lastAccessedAt, lp.completedAt, lp.startedAt) >= :startDate " +
           "GROUP BY CAST(COALESCE(lp.lastAccessedAt, lp.completedAt, lp.startedAt) AS date) " +
           "ORDER BY CAST(COALESCE(lp.lastAccessedAt, lp.completedAt, lp.startedAt) AS date) ASC")
    List<Object[]> findDailyActivityCountsByStudentId(@Param("studentId") UUID studentId, @Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT lp.student.id, COUNT(lp), MAX(lp.lastAccessedAt) " +
           "FROM LessonProgress lp JOIN lp.lesson l JOIN l.chapter ch " +
           "WHERE ch.course.id = :courseId AND lp.status = com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus.COMPLETED " +
           "GROUP BY lp.student.id")
    List<Object[]> countCompletedLessonsByStudentForCourse(@Param("courseId") UUID courseId);
}
