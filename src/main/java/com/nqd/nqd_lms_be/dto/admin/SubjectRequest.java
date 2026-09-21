package com.nqd.nqd_lms_be.dto.admin;

import com.nqd.nqd_lms_be.entity.enums.SubjectStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectRequest {
    @NotBlank(message = "Subject name is required")
    private String name;

    @NotBlank(message = "Subject code is required")
    private String code;

    private String description;
    private SubjectStatus status;
}
