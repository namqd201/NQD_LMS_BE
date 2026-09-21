package com.nqd.nqd_lms_be.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectTeacherApplicationRequest {
    @NotBlank(message = "Vui lòng nhập lý do từ chối")
    private String reason;
}
