package com.nqd.nqd_lms_be.config.security;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicRoleAuthenticationFilter extends OncePerRequestFilter {

    private final UserRoleRepository userRoleRepository;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

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
