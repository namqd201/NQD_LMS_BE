package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt >= :since")
    long countLogsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT a.user.id) FROM AuditLog a WHERE a.createdAt >= :since AND a.user IS NOT NULL")
    long countActiveUsersSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.createdAt >= :since GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> findTopActionsSince(@Param("since") LocalDateTime since);
}
