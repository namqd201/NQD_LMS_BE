package com.nqd.nqd_lms_be.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDetailResponse {
    private UUID id;
    private String name;
    private String description;
    private long userCount;
}
