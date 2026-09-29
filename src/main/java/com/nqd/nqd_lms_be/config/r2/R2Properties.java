package com.nqd.nqd_lms_be.config.r2;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cloudflare.r2")
public class R2Properties {
    private String accountId = "";
    private String accessKey = "";
    private String secretKey = "";
    private String bucketName = "nqd-lms-recordings";
    private String publicUrl = "https://pub-r2.nqdlms.online";
    private boolean enabled = false;

    public boolean isConfigured() {
        return enabled && accountId != null && !accountId.isBlank()
                && accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }
}
