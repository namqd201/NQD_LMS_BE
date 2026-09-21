package com.nqd.nqd_lms_be.billing.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookVerificationCommand {

    private String rawPayload;
    private String signature;
    private Map<String, String> headers;
}
