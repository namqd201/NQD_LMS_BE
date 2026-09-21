package com.nqd.nqd_lms_be.dto;

import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String phoneNumber;
    private UserStatus status;
    private Set<String> roles;
    private Boolean isOnboarded;
}
