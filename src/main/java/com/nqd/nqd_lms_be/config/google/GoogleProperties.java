package com.nqd.nqd_lms_be.config.google;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "google")
public class GoogleProperties {
    private String clientId;
    private String clientSecret;
    private String refreshToken;
    private Meet meet = new Meet();

    @Data
    public static class Meet {
        private boolean enabled = true;
        private String driveFolderName = "Meet Recordings";
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && refreshToken != null && !refreshToken.isBlank();
    }
}
