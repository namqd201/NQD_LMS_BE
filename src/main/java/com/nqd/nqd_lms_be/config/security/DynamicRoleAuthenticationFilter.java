package com.nqd.nqd_lms_be.config.security;

import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.UserStatus;
import com.nqd.nqd_lms_be.repository.UserRepository;
import com.nqd.nqd_lms_be.repository.UserRoleRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicRoleAuthenticationFilter extends OncePerRequestFilter {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final SessionAuthRegistry sessionAuthRegistry;
    private final JwtService jwtService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            String token = request.getHeader("X-Session-Id");
            if (token == null || token.isBlank()) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7).trim();
                }
            }

            if (token != null && !token.isBlank()) {
                // 1. Verify and authenticate with stateless 7-day JWT
                if (jwtService.validateToken(token)) {
                    UUID userId = jwtService.getUserIdFromToken(token);
                    if (userId != null) {
                        User user = userRepository.findById(userId).orElse(null);
                        if (user != null && user.getStatus() != UserStatus.BANNED) {
                            List<String> latestRoleNames = userRoleRepository.findRoleNamesByUserId(userId);
                            Set<String> roles = (latestRoleNames != null && !latestRoleNames.isEmpty())
                                    ? new HashSet<>(latestRoleNames)
                                    : jwtService.getRolesFromToken(token);

                            AppUserPrincipal principal = AppUserPrincipal.create(user, roles, Collections.emptyMap());
                            OAuth2AuthenticationToken jwtAuth = new OAuth2AuthenticationToken(
                                    principal,
                                    principal.getAuthorities(),
                                    "google"
                            );

                            SecurityContext context = SecurityContextHolder.createEmptyContext();
                            context.setAuthentication(jwtAuth);
                            SecurityContextHolder.setContext(context);
                            authentication = jwtAuth;
                        }
                    }
                }

                // 2. Fallback to in-memory sessionAuthRegistry for legacy session tokens
                if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                    Authentication registeredAuth = sessionAuthRegistry.get(token);
                    if (registeredAuth != null) {
                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(registeredAuth);
                        SecurityContextHolder.setContext(context);
                        authentication = registeredAuth;
                    }
                }
            }
        }

        // 3. Dynamic role refresh if user roles have changed in DB
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            UUID userId = principal.getId();
            List<String> latestRoleNames = userRoleRepository.findRoleNamesByUserId(userId);
            if (latestRoleNames != null && !latestRoleNames.isEmpty()) {
                Set<String> latestRoles = new HashSet<>(latestRoleNames);
                Set<String> currentRoles = principal.getRoles();
                if (!currentRoles.equals(latestRoles)) {
                    log.info("Refreshing dynamic authorities for user {}: {} -> {}", principal.getEmail(), currentRoles, latestRoles);

                    AppUserPrincipal updatedPrincipal = principal.withRoles(latestRoles);

                    String clientRegistrationId = "google";
                    if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                        clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
                    }

                    OAuth2AuthenticationToken updatedAuth = new OAuth2AuthenticationToken(
                            updatedPrincipal,
                            updatedPrincipal.getAuthorities(),
                            clientRegistrationId
                    );

                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(updatedAuth);
                    SecurityContextHolder.setContext(context);
                    securityContextRepository.saveContext(context, request, response);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
