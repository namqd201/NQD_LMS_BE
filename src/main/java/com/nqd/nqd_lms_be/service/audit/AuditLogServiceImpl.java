package com.nqd.nqd_lms_be.service.audit;

import com.nqd.nqd_lms_be.dto.audit.AuditLogDto;
import com.nqd.nqd_lms_be.entity.AuditLog;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.repository.AuditLogRepository;
import com.nqd.nqd_lms_be.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import com.nqd.nqd_lms_be.repository.UserRoleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Async("applicationTaskExecutor")
    @Transactional
    public void logAsync(UUID userId, String action, String entityType, UUID entityId, String details, String ipAddress, String userAgent) {
        try {
            User user = null;
            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
            }

            AuditLog logEntry = AuditLog.builder()
                    .user(user)
                    .action(action != null ? action.trim().toUpperCase() : "UNKNOWN")
                    .entityType(entityType != null ? entityType.trim() : "System")
                    .entityId(entityId)
                    .details(details)
                    .ipAddress(ipAddress != null && ipAddress.length() > 45 ? ipAddress.substring(0, 45) : ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(logEntry);
            log.debug("AuditLog recorded: action={}, user={}, entity={}", action, userId, entityType);
        } catch (Exception e) {
            log.error("Failed to record audit log for action '{}': {}", action, e.getMessage());
        }
    }

    @Override
    @Async("applicationTaskExecutor")
    @Transactional
    public void logAsync(User user, String action, String entityType, UUID entityId, String details, HttpServletRequest request) {
        String ip = extractClientIp(request);
        String ua = extractUserAgent(request);
        UUID userId = user != null ? user.getId() : null;
        logAsync(userId, action, entityType, entityId, details, ip, ua);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto.Response> getAuditLogs(AuditLogDto.FilterRequest filter) {
        int page = Math.max(0, filter.getPage());
        int size = Math.max(1, Math.min(100, filter.getSize()));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filter by specific user
            if (filter.getUserId() != null) {
                predicates.add(cb.equal(root.get("user").get("id"), filter.getUserId()));
            }

            // 2. Filter by action
            if (filter.getAction() != null && !filter.getAction().isBlank()) {
                predicates.add(cb.equal(root.get("action"), filter.getAction().trim().toUpperCase()));
            }

            // 3. Filter by entityType
            if (filter.getEntityType() != null && !filter.getEntityType().isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), filter.getEntityType().trim()));
            }

            // 4. Search term (user fullName, email, details)
            if (filter.getSearchTerm() != null && !filter.getSearchTerm().isBlank()) {
                String term = "%" + filter.getSearchTerm().trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("user").get("fullName")), term);
                Predicate emailMatch = cb.like(cb.lower(root.get("user").get("email")), term);
                Predicate detailsMatch = cb.like(cb.lower(root.get("details")), term);
                predicates.add(cb.or(nameMatch, emailMatch, detailsMatch));
            }

            // 5. Date filter: specific day
            if (filter.getDate() != null) {
                LocalDateTime startOfDay = filter.getDate().atStartOfDay();
                LocalDateTime endOfDay = filter.getDate().atTime(LocalTime.MAX);
                predicates.add(cb.between(root.get("createdAt"), startOfDay, endOfDay));
            }
            // 6. Month filter: specific month & year
            else if (filter.getYear() != null && filter.getMonth() != null) {
                YearMonth ym = YearMonth.of(filter.getYear(), filter.getMonth());
                LocalDateTime startOfMonth = ym.atDay(1).atStartOfDay();
                LocalDateTime endOfMonth = ym.atEndOfMonth().atTime(LocalTime.MAX);
                predicates.add(cb.between(root.get("createdAt"), startOfMonth, endOfMonth));
            }
            // 7. Custom range
            else {
                if (filter.getStartDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate()));
                }
                if (filter.getEndDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuditLog> pageResult = auditLogRepository.findAll(spec, pageable);
        return pageResult.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogDto.StatsResponse getAuditStats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long totalToday = auditLogRepository.countLogsSince(startOfToday);
        long activeUsersToday = auditLogRepository.countActiveUsersSince(startOfToday);

        List<Object[]> actionRows = auditLogRepository.findTopActionsSince(startOfToday);
        List<AuditLogDto.ActionCount> topActions = actionRows.stream()
                .map(row -> AuditLogDto.ActionCount.builder()
                        .action((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .limit(5)
                .collect(Collectors.toList());

        return AuditLogDto.StatsResponse.builder()
                .totalLogsToday(totalToday)
                .activeUsersToday(activeUsersToday)
                .topActionsToday(topActions)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto.Response> getUserTimeline(UUID userId, LocalDate date, Integer year, Integer month) {
        AuditLogDto.FilterRequest filter = AuditLogDto.FilterRequest.builder()
                .userId(userId)
                .date(date)
                .year(year)
                .month(month)
                .page(0)
                .size(100)
                .build();

        return getAuditLogs(filter).getContent();
    }

    private AuditLogDto.Response mapToResponse(AuditLog log) {
        User u = log.getUser();
        String roleStr = null;
        if (u != null && u.getId() != null) {
            try {
                List<String> roles = userRoleRepository.findRoleNamesByUserId(u.getId());
                if (roles != null && !roles.isEmpty()) {
                    roleStr = String.join(", ", roles);
                }
            } catch (Exception ignored) {
            }
        }

        return AuditLogDto.Response.builder()
                .id(log.getId())
                .userId(u != null ? u.getId() : null)
                .userName(u != null ? u.getFullName() : "Hệ thống / Ẩn danh")
                .userEmail(u != null ? u.getEmail() : null)
                .userAvatarUrl(u != null ? u.getAvatarUrl() : null)
                .userRole(roleStr)
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) return null;
        return request.getHeader("User-Agent");
    }
}
