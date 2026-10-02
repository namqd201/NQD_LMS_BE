package com.nqd.nqd_lms_be.dto.classroom;

import com.nqd.nqd_lms_be.entity.enums.ClassroomStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClassroomStatusRequest {

    @NotNull(message = "Trạng thái lớp học không được để trống")
    private ClassroomStatus status;
}
