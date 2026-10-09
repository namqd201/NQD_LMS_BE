package com.nqd.nqd_lms_be.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AuditLogDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID userId;
        private String userName;
        private String userEmail;
        private String userAvatarUrl;
        private String userRole;
        private String action;
        private String entityType;
        private UUID entityId;
        private String details;
        private String ipAddress;
        private String userAgent;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterRequest {
        private UUID userId;
        private String action;
        private String entityType;
        private String searchTerm;
        private LocalDate date;
        private Integer year;
        private Integer month;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        @Builder.Default
        private int page = 0;
        @Builder.Default
        private int size = 20;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsResponse {
        private long totalLogsToday;
        private long activeUsersToday;
        private List<ActionCount> topActionsToday;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionCount {
        private String action;
        private long count;
    }
}
