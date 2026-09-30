package com.nqd.nqd_lms_be.config.onehundredms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "onehundredms")
public class OneHundredMsProperties {
    private boolean enabled = true;
    private String appAccessKey = "6abd9119692d18ad401539d2";
    private String appSecret = "SXMBlWkoW75XYlvsy-LzzVFSo1xLwsgHYs9dhC1oBicLuSEbV_YmXMONMTJANOri1WP-rzKfH9GbvV5xpLyv-mTfIRtYqFnQ_D63BuAvHDEr_DU1SycoexykEDEidI-DXxc20SCkkE1xtLSTiMhXIaxLOaMVL9Jv87Za4ChY9-4=";
    private String templateId = "6abd927f06e552af73ba8f24";
    private String subdomain = "small-forest-267978";
    private String apiBaseUrl = "https://api.100ms.live/v2";
}
