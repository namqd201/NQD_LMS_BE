package com.nqd.nqd_lms_be.dto.classroom;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteStudentRequest {

    @NotBlank(message = "Email học sinh không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    private String message;
}
