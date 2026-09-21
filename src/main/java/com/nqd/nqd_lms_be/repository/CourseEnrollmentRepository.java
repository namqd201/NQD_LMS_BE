package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.CourseEnrollment;
import com.nqd.nqd_lms_be.entity.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {
    List<CourseEnrollment> findByStudentId(UUID studentId);
    List<CourseEnrollment> findByCourseId(UUID courseId);
    List<CourseEnrollment> findByCourseIdAndStatus(UUID courseId, EnrollmentStatus status);
    Optional<CourseEnrollment> findByIdAndCourseId(UUID id, UUID courseId);
    Optional<CourseEnrollment> findByCourseIdAndStudentId(UUID courseId, UUID studentId);
    boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId);
    long countByCourseId(UUID courseId);
    long countByCourseIdAndStatus(UUID courseId, EnrollmentStatus status);
    long countByStudentId(UUID studentId);
    long countByStudentIdAndStatus(UUID studentId, EnrollmentStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT ce.status, COUNT(ce) FROM CourseEnrollment ce WHERE ce.course.id = :courseId GROUP BY ce.status")
    List<Object[]> countEnrollmentsByStatusForCourse(@org.springframework.data.repository.query.Param("courseId") UUID courseId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT ce FROM CourseEnrollment ce JOIN FETCH ce.student WHERE ce.course.id = :courseId")
    List<CourseEnrollment> findByCourseIdWithStudent(@org.springframework.data.repository.query.Param("courseId") UUID courseId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT ce FROM CourseEnrollment ce JOIN FETCH ce.course WHERE ce.student.id = :studentId")
    List<CourseEnrollment> findByStudentIdWithCourse(@org.springframework.data.repository.query.Param("studentId") UUID studentId);
}
