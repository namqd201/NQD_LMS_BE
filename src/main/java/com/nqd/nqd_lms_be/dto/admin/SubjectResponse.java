package com.nqd.nqd_lms_be.dto.admin;

import com.nqd.nqd_lms_be.entity.enums.SubjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectResponse {
    private UUID id;
    private String name;
    private String code;
    private String description;
    private SubjectStatus status;
    private long courseCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private String deletedBy;
}
