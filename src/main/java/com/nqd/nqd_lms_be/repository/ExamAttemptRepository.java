package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ExamAttempt;
import com.nqd.nqd_lms_be.entity.enums.ExamAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {
    List<ExamAttempt> findByStudentId(UUID studentId);
    List<ExamAttempt> findByExamId(UUID examId);
    List<ExamAttempt> findByExamIdOrderBySubmittedAtDescStartedAtDesc(UUID examId);
    Optional<ExamAttempt> findByExamIdAndStudentIdAndAttemptNumber(UUID examId, UUID studentId, Integer attemptNumber);
    List<ExamAttempt> findByExamIdAndStudentIdOrderByAttemptNumberAsc(UUID examId, UUID studentId);
    long countByExamIdAndStudentId(UUID examId, UUID studentId);
    long countByExamId(UUID examId);
    long countByExamIdAndStatus(UUID examId, ExamAttemptStatus status);
    long countByStudentIdAndStatus(UUID studentId, ExamAttemptStatus status);
    long countByStudentIdAndPassed(UUID studentId, Boolean passed);

    @org.springframework.data.jpa.repository.Query("SELECT ea FROM ExamAttempt ea WHERE ea.student.id = :studentId AND ea.exam.course.id = :courseId ORDER BY ea.startedAt DESC")
    List<ExamAttempt> findByStudentIdAndCourseId(@org.springframework.data.repository.query.Param("studentId") UUID studentId, @org.springframework.data.repository.query.Param("courseId") UUID courseId);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(ea.percentage) FROM ExamAttempt ea WHERE ea.student.id = :studentId AND ea.status = com.nqd.nqd_lms_be.entity.enums.ExamAttemptStatus.SUBMITTED AND ea.percentage IS NOT NULL")
    Double findAveragePercentageByStudentId(@org.springframework.data.repository.query.Param("studentId") UUID studentId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(ea), AVG(ea.percentage), " +
           "SUM(CASE WHEN ea.passed = true OR (ea.passed IS NULL AND ea.percentage >= 50.0) THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN ea.passed = false OR (ea.passed IS NULL AND ea.percentage < 50.0) THEN 1 ELSE 0 END) " +
           "FROM ExamAttempt ea WHERE ea.exam.course.id = :courseId AND ea.status = com.nqd.nqd_lms_be.entity.enums.ExamAttemptStatus.SUBMITTED")
    List<Object[]> findExamStatsByCourseId(@org.springframework.data.repository.query.Param("courseId") UUID courseId);

    @org.springframework.data.jpa.repository.Query("SELECT " +
           "SUM(CASE WHEN ea.percentage < 50.0 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN ea.percentage >= 50.0 AND ea.percentage < 70.0 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN ea.percentage >= 70.0 AND ea.percentage < 85.0 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN ea.percentage >= 85.0 THEN 1 ELSE 0 END) " +
           "FROM ExamAttempt ea WHERE ea.exam.course.id = :courseId AND ea.status = com.nqd.nqd_lms_be.entity.enums.ExamAttemptStatus.SUBMITTED")
    List<Object[]> findScoreDistributionByCourseId(@org.springframework.data.repository.query.Param("courseId") UUID courseId);

    @org.springframework.data.jpa.repository.Query("SELECT CAST(ea.submittedAt AS date), COUNT(ea) " +
           "FROM ExamAttempt ea WHERE ea.exam.course.id = :courseId AND ea.submittedAt IS NOT NULL AND ea.submittedAt >= :startDate " +
           "GROUP BY CAST(ea.submittedAt AS date) " +
           "ORDER BY CAST(ea.submittedAt AS date) ASC")
    List<Object[]> findDailyExamSubmissionsByCourseId(@org.springframework.data.repository.query.Param("courseId") UUID courseId, @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate);

    @org.springframework.data.jpa.repository.Query("SELECT ea.student.id, AVG(ea.percentage) " +
           "FROM ExamAttempt ea WHERE ea.exam.course.id = :courseId AND ea.status = com.nqd.nqd_lms_be.entity.enums.ExamAttemptStatus.SUBMITTED AND ea.percentage IS NOT NULL " +
           "GROUP BY ea.student.id")
    List<Object[]> findAverageScoreByStudentForCourse(@org.springframework.data.repository.query.Param("courseId") UUID courseId);
}
