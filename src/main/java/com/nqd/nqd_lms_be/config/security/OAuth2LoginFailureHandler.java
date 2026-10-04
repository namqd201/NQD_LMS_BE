package com.nqd.nqd_lms_be.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final String frontendUrl;

    public OAuth2LoginFailureHandler(@Value("${app.frontend.url:http://localhost:3000}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        String redirectBase = session != null ? (String) session.getAttribute("OAUTH2_REDIRECT_ORIGIN") : null;
        if (redirectBase == null || redirectBase.isBlank() || !isValidFrontendOrigin(redirectBase)) {
            redirectBase = frontendUrl;
        }
        if (redirectBase == null || redirectBase.isBlank()) {
            redirectBase = "https://nqdlms.online";
        }

        String errorCode = "oauth2_failure";
        String reason = "";

        if (exception instanceof OAuth2AuthenticationException oauthEx) {
            if ("account_locked".equalsIgnoreCase(oauthEx.getError().getErrorCode())) {
                errorCode = "account_locked";
                reason = oauthEx.getError().getDescription();
                if (reason == null || reason.isBlank()) {
                    reason = oauthEx.getMessage();
                }
            } else if (oauthEx.getMessage() != null && oauthEx.getMessage().toLowerCase().contains("khóa")) {
                errorCode = "account_locked";
                reason = oauthEx.getMessage();
            }
        } else if (exception != null && exception.getMessage() != null && exception.getMessage().toLowerCase().contains("khóa")) {
            errorCode = "account_locked";
            reason = exception.getMessage();
        }

        if (reason != null && reason.startsWith("[account_locked] ")) {
            reason = reason.substring("[account_locked] ".length());
        }

        String targetUrl = redirectBase + "/login?error=" + errorCode;
        if (reason != null && !reason.isBlank()) {
            targetUrl += "&reason=" + URLEncoder.encode(reason, StandardCharsets.UTF_8);
        }

        log.warn("OAuth2 login failed: error={}, reason={}. Redirecting to {}", errorCode, reason, targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private boolean isValidFrontendOrigin(String origin) {
        if (origin == null) return false;
        String lower = origin.toLowerCase();
        return lower.contains("localhost") ||
               lower.contains("127.0.0.1") ||
               lower.contains("nqdlms.online") ||
               lower.endsWith(".vercel.app") ||
               lower.endsWith(".onrender.com");
    }
}
