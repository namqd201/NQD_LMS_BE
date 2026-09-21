package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.TeacherWithdrawal;
import com.nqd.nqd_lms_be.entity.enums.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherWithdrawalRepository extends JpaRepository<TeacherWithdrawal, UUID> {

    Page<TeacherWithdrawal> findByTeacherIdAndIsDeletedFalseOrderByRequestedAtDesc(UUID teacherId, Pageable pageable);

    Page<TeacherWithdrawal> findByStatusAndIsDeletedFalseOrderByRequestedAtDesc(WithdrawalStatus status, Pageable pageable);

    Page<TeacherWithdrawal> findAllByIsDeletedFalseOrderByRequestedAtDesc(Pageable pageable);

    Optional<TeacherWithdrawal> findByIdempotencyKey(String idempotencyKey);

    Optional<TeacherWithdrawal> findByWithdrawalCode(String withdrawalCode);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM TeacherWithdrawal w WHERE w.teacher.id = :teacherId AND w.status IN ('PENDING', 'PROCESSING', 'COMPLETED') AND w.isDeleted = false")
    BigDecimal sumCommittedWithdrawalsByTeacherId(@Param("teacherId") UUID teacherId);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM TeacherWithdrawal w WHERE w.teacher.id = :teacherId AND w.status = 'COMPLETED' AND w.isDeleted = false")
    BigDecimal sumCompletedWithdrawalsByTeacherId(@Param("teacherId") UUID teacherId);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM TeacherWithdrawal w WHERE w.status = 'COMPLETED' AND w.isDeleted = false")
    BigDecimal sumPlatformTotalCompletedWithdrawals();

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM TeacherWithdrawal w WHERE w.status IN ('PENDING', 'PROCESSING') AND w.isDeleted = false")
    BigDecimal sumPlatformTotalPendingWithdrawals();
}
