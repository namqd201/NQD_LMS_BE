package com.nqd.nqd_lms_be.dto.freegrant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuotaRequest {
    @NotNull(message = "Số suất xin thêm không được để trống")
    @Min(value = 1, message = "Số suất xin thêm tối thiểu là 1")
    private Integer requestedQuota;

    @NotBlank(message = "Lý do xin thêm không được để trống")
    private String reason;
}
