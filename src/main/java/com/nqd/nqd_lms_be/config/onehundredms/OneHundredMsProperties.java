package com.nqd.nqd_lms_be.config.onehundredms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "onehundredms")
public class OneHundredMsProperties {
    private boolean enabled = true;
    private String appAccessKey;
    private String appSecret;
    private String templateId;
    private String subdomain;
    private String apiBaseUrl = "https://api.100ms.live/v2";
}
