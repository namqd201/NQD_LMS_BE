package com.nqd.nqd_lms_be.dto.classroom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSuggestionResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String roles;
}
