package com.nqd.nqd_lms_be.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final String frontendUrl;
    private final SessionAuthRegistry sessionAuthRegistry;

    public OAuth2LoginSuccessHandler(@Value("${app.frontend.url:http://localhost:3000}") String frontendUrl,
                                     SessionAuthRegistry sessionAuthRegistry) {
        this.frontendUrl = frontendUrl;
        this.sessionAuthRegistry = sessionAuthRegistry;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession(true);
        String sessionId = session != null ? session.getId() : "";
        if (!sessionId.isBlank()) {
            sessionAuthRegistry.register(sessionId, authentication);
        }

        String targetUrl = frontendUrl + "/auth/callback?session_id=" + sessionId;
        log.info("OAuth2 login success, redirecting to frontend: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
