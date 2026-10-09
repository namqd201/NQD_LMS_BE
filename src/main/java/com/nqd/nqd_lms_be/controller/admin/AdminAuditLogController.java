package com.nqd.nqd_lms_be.controller.admin;

import com.nqd.nqd_lms_be.dto.audit.AuditLogDto;
import com.nqd.nqd_lms_be.service.audit.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Admin - Audit Logs", description = "Endpoints for inspecting user activity logs and security audits")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Get paginated audit logs with flexible filtering by user, date, month, and action")
    public ResponseEntity<Page<AuditLogDto.Response>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        AuditLogDto.FilterRequest filter = AuditLogDto.FilterRequest.builder()
                .page(page)
                .size(size)
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .searchTerm(searchTerm)
                .date(date)
                .year(year)
                .month(month)
                .build();

        return ResponseEntity.ok(auditLogService.getAuditLogs(filter));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get audit logs summary statistics for admin dashboard")
    public ResponseEntity<AuditLogDto.StatsResponse> getAuditStats() {
        return ResponseEntity.ok(auditLogService.getAuditStats());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get activity timeline for a specific user on a specific date or month")
    public ResponseEntity<List<AuditLogDto.Response>> getUserTimeline(
            @PathVariable UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(auditLogService.getUserTimeline(userId, date, year, month));
    }
}
