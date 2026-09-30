package com.nqd.nqd_lms_be.service.google;

public interface GoogleOAuthService {
    boolean isConfigured();
    String getFreshAccessToken();
}
