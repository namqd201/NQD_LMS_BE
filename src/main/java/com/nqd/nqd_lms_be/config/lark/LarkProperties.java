package com.nqd.nqd_lms_be.config.lark;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "lark")
public class LarkProperties {
    private String appId = "";
    private String appSecret = "";
    private boolean enabled = true;
    private String baseUrl = "https://open.larksuite.com";
}
