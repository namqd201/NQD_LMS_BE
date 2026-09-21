package com.nqd.nqd_lms_be.dto.admin;

import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String phoneNumber;
    private UserStatus status;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private String currentPlanCode;
    private String currentPlanName;
    private Boolean isVip;
    private LocalDateTime subscriptionEndDate;
}
