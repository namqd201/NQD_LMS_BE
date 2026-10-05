package com.nqd.nqd_lms_be.dto.push;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscriptionDto {

    @NotBlank(message = "Endpoint cannot be blank")
    private String endpoint;

    @NotBlank(message = "p256dh key cannot be blank")
    private String p256dh;

    @NotBlank(message = "auth key cannot be blank")
    private String auth;

    private String userAgent;
}
