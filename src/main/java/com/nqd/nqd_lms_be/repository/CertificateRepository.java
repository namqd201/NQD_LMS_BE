package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    Optional<Certificate> findByCertificateCode(String certificateCode);

    @Query("SELECT c FROM Certificate c WHERE c.student.id = :studentId AND c.course.id = :courseId AND (c.isDeleted IS NULL OR c.isDeleted = false)")
    Optional<Certificate> findByStudentIdAndCourseId(@Param("studentId") UUID studentId, @Param("courseId") UUID courseId);

    @Query("SELECT c FROM Certificate c WHERE c.student.id = :studentId AND (c.isDeleted IS NULL OR c.isDeleted = false) ORDER BY c.issuedAt DESC")
    List<Certificate> findByStudentIdOrderByIssuedAtDesc(@Param("studentId") UUID studentId);

    @Query("SELECT COUNT(c) > 0 FROM Certificate c WHERE c.student.id = :studentId AND c.course.id = :courseId AND (c.isDeleted IS NULL OR c.isDeleted = false)")
    boolean existsByStudentIdAndCourseId(@Param("studentId") UUID studentId, @Param("courseId") UUID courseId);

    @Query("SELECT COUNT(c) FROM Certificate c WHERE c.course.id = :courseId AND (c.isDeleted IS NULL OR c.isDeleted = false)")
    long countByCourseId(@Param("courseId") UUID courseId);
}
