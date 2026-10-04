package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.GamificationXpLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface GamificationXpLogRepository extends JpaRepository<GamificationXpLog, UUID> {

    List<GamificationXpLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(l.xpAmount), 0) FROM GamificationXpLog l WHERE l.user.id = :userId AND l.createdAt >= :since")
    int sumXpSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    boolean existsByUserIdAndActionTypeAndReferenceId(UUID userId, com.nqd.nqd_lms_be.entity.enums.GamificationActionType actionType, String referenceId);
}
