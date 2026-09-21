package com.nqd.nqd_lms_be.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCourseDisableRequest {
    @NotBlank(message = "Reason for disabling course is required")
    private String reason;
}
