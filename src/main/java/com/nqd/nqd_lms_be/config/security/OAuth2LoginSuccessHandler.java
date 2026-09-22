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

        String redirectBase = (String) session.getAttribute("OAUTH2_REDIRECT_ORIGIN");
        if (redirectBase == null || redirectBase.isBlank() || !isValidFrontendOrigin(redirectBase)) {
            redirectBase = frontendUrl;
        }
        if (redirectBase == null || redirectBase.isBlank()) {
            redirectBase = "https://nqdlms.online";
        }

        String targetUrl = redirectBase + "/auth/callback?token=" + finalToken + "&session_id=" + finalToken;
        log.info("OAuth2 login success, redirecting to frontend ({}) with JWT: {}...", redirectBase, finalToken.substring(0, Math.min(finalToken.length(), 15)));
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
