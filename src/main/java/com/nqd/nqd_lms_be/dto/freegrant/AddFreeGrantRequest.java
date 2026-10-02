package com.nqd.nqd_lms_be.dto.freegrant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddFreeGrantRequest {
    @NotBlank(message = "Email học sinh không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;
}
