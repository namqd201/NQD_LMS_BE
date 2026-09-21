package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.TeacherEarning;
import com.nqd.nqd_lms_be.entity.enums.EarningStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherEarningRepository extends JpaRepository<TeacherEarning, UUID> {

    Page<TeacherEarning> findByTeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID teacherId, Pageable pageable);

    Page<TeacherEarning> findByTeacherIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(UUID teacherId, EarningStatus status, Pageable pageable);

    Optional<TeacherEarning> findByOrderItemIdAndIsDeletedFalse(UUID orderItemId);

    List<TeacherEarning> findByOrderIdAndIsDeletedFalse(UUID orderId);

    @Query("SELECT COALESCE(SUM(e.teacherAmount), 0) FROM TeacherEarning e WHERE e.teacher.id = :teacherId AND e.status != 'REVERSED' AND e.isDeleted = false")
    BigDecimal sumTotalEarnedByTeacherId(@Param("teacherId") UUID teacherId);

    @Query("SELECT COALESCE(SUM(e.teacherAmount), 0) FROM TeacherEarning e WHERE e.teacher.id = :teacherId AND (e.status = 'AVAILABLE' OR (e.status = 'PENDING' AND e.availableAt <= :now)) AND e.isDeleted = false")
    BigDecimal sumAvailableEarningByTeacherId(@Param("teacherId") UUID teacherId, @Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(e.teacherAmount), 0) FROM TeacherEarning e WHERE e.teacher.id = :teacherId AND e.status = 'PENDING' AND e.availableAt > :now AND e.isDeleted = false")
    BigDecimal sumPendingEarningByTeacherId(@Param("teacherId") UUID teacherId, @Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(e.teacherAmount), 0) FROM TeacherEarning e WHERE e.teacher.id = :teacherId AND e.status = 'REVERSED' AND e.isDeleted = false")
    BigDecimal sumReversedEarningByTeacherId(@Param("teacherId") UUID teacherId);

    // Platform analytics
    @Query("SELECT COALESCE(SUM(e.grossAmount), 0) FROM TeacherEarning e WHERE e.status != 'REVERSED' AND e.isDeleted = false")
    BigDecimal sumPlatformGrossRevenue();

    @Query("SELECT COALESCE(SUM(e.platformFee), 0) FROM TeacherEarning e WHERE e.status != 'REVERSED' AND e.isDeleted = false")
    BigDecimal sumPlatformTotalFees();

    @Query("SELECT COALESCE(SUM(e.teacherAmount), 0) FROM TeacherEarning e WHERE e.status != 'REVERSED' AND e.isDeleted = false")
    BigDecimal sumPlatformTotalTeacherPayouts();

    @Query("SELECT COALESCE(SUM(e.grossAmount), 0) FROM TeacherEarning e WHERE e.status = 'REVERSED' AND e.isDeleted = false")
    BigDecimal sumPlatformReversedAmount();
}
