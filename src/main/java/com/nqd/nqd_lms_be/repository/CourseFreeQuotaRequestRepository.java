package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.CourseFreeQuotaRequest;
import com.nqd.nqd_lms_be.entity.enums.FreeQuotaRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseFreeQuotaRequestRepository extends JpaRepository<CourseFreeQuotaRequest, UUID> {

    Page<CourseFreeQuotaRequest> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<CourseFreeQuotaRequest> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(FreeQuotaRequestStatus status, Pageable pageable);

    List<CourseFreeQuotaRequest> findByCourseIdAndTeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID courseId, UUID teacherId);

    boolean existsByCourseIdAndTeacherIdAndStatusAndIsDeletedFalse(UUID courseId, UUID teacherId, FreeQuotaRequestStatus status);

    Optional<CourseFreeQuotaRequest> findByIdAndIsDeletedFalse(UUID id);
}
