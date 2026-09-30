package com.nqd.nqd_lms_be.config.youtube;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "youtube")
public class YouTubeProperties {
    private String clientId = "";
    private String clientSecret = "";
    private String refreshToken = "";
    private boolean enabled = true;

    public boolean isConfigured() {
        return enabled && clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && refreshToken != null && !refreshToken.isBlank();
    }
}
