package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.TeacherApplication;
import com.nqd.nqd_lms_be.entity.enums.TeacherApplicantType;
import com.nqd.nqd_lms_be.entity.enums.TeacherApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TeacherApplicationRepository extends JpaRepository<TeacherApplication, UUID> {

    Optional<TeacherApplication> findTopByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndStatusAndIsDeletedFalse(UUID userId, TeacherApplicationStatus status);

    long countByStatusAndIsDeletedFalse(TeacherApplicationStatus status);

    @Query(value = """
        SELECT a FROM TeacherApplication a
        JOIN FETCH a.user
        LEFT JOIN FETCH a.reviewedBy
        WHERE a.isDeleted = false
          AND (:status IS NULL OR a.status = :status)
          AND (:applicantType IS NULL OR a.applicantType = :applicantType)
          AND (
              :keyword IS NULL OR :keyword = ''
              OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(a.institutionName) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(a.majorOrSubject) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        ORDER BY a.createdAt DESC
    """, countQuery = """
        SELECT COUNT(a) FROM TeacherApplication a
        WHERE a.isDeleted = false
          AND (:status IS NULL OR a.status = :status)
          AND (:applicantType IS NULL OR a.applicantType = :applicantType)
          AND (
              :keyword IS NULL OR :keyword = ''
              OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(a.institutionName) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(a.majorOrSubject) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    Page<TeacherApplication> searchApplications(
            @Param("status") TeacherApplicationStatus status,
            @Param("applicantType") TeacherApplicantType applicantType,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
