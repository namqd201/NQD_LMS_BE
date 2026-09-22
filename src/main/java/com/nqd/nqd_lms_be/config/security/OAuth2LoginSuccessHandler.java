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
    private final JwtService jwtService;

    public OAuth2LoginSuccessHandler(@Value("${app.frontend.url:http://localhost:3000}") String frontendUrl,
                                     SessionAuthRegistry sessionAuthRegistry,
                                     JwtService jwtService) {
        this.frontendUrl = frontendUrl;
        this.sessionAuthRegistry = sessionAuthRegistry;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession(true);
        String sessionId = session != null ? session.getId() : "";
        if (!sessionId.isBlank()) {
            sessionAuthRegistry.register(sessionId, authentication);
        }

        String jwtToken = "";
        if (authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            jwtToken = jwtService.generateToken(
                    principal.getId(),
                    principal.getEmail(),
                    principal.getFullName(),
                    principal.getRoles()
            );
            if (!jwtToken.isBlank()) {
                sessionAuthRegistry.register(jwtToken, authentication);
            }
        }

        String finalToken = !jwtToken.isBlank() ? jwtToken : sessionId;
        String targetUrl = frontendUrl + "/auth/callback?token=" + finalToken + "&session_id=" + finalToken;
        log.info("OAuth2 login success, redirecting to frontend with JWT: {}...", finalToken.substring(0, Math.min(finalToken.length(), 15)));
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
