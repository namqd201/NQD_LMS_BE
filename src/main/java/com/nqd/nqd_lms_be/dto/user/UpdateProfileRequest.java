package com.nqd.nqd_lms_be.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    @NotBlank(message = "Full name cannot be blank")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String phoneNumber;

    private String avatarUrl;
}
