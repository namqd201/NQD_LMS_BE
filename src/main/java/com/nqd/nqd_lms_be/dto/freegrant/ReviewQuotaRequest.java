package com.nqd.nqd_lms_be.dto.freegrant;

import com.nqd.nqd_lms_be.entity.enums.FreeQuotaRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewQuotaRequest {
    @NotNull(message = "Trạng thái phê duyệt không được để trống")
    private FreeQuotaRequestStatus status;

    private String adminNote;
}
