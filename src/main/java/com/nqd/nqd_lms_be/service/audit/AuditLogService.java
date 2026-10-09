package com.nqd.nqd_lms_be.service.audit;

import com.nqd.nqd_lms_be.dto.audit.AuditLogDto;
import com.nqd.nqd_lms_be.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AuditLogService {

    /**
     * Asynchronously record an activity log entry.
     */
    void logAsync(UUID userId, String action, String entityType, UUID entityId, String details, String ipAddress, String userAgent);

    /**
     * Asynchronously record an activity log entry with User entity and HTTP Request metadata.
     */
    void logAsync(User user, String action, String entityType, UUID entityId, String details, HttpServletRequest request);

    /**
     * Retrieve paginated audit logs based on search filters (user, date, action, etc.).
     */
    Page<AuditLogDto.Response> getAuditLogs(AuditLogDto.FilterRequest filter);

    /**
     * Retrieve statistics for admin dashboard.
     */
    AuditLogDto.StatsResponse getAuditStats();

    /**
     * Retrieve timeline of a specific user on a specific date or month.
     */
    List<AuditLogDto.Response> getUserTimeline(UUID userId, LocalDate date, Integer year, Integer month);
}
