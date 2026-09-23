package com.nqd.nqd_lms_be.config;

import com.nqd.nqd_lms_be.config.security.CustomOAuth2UserService;
import com.nqd.nqd_lms_be.config.security.CustomOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final com.nqd.nqd_lms_be.config.security.DynamicRoleAuthenticationFilter dynamicRoleAuthenticationFilter;
    private final com.nqd.nqd_lms_be.config.security.OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Value("${app.frontend.url:https://nqdlms.online}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .addFilterBefore(dynamicRoleAuthenticationFilter, org.springframework.security.web.access.intercept.AuthorizationFilter.class)
            .addFilterBefore(new org.springframework.web.filter.OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                                jakarta.servlet.http.HttpServletResponse response,
                                                jakarta.servlet.FilterChain filterChain)
                        throws jakarta.servlet.ServletException, java.io.IOException {
                    String uri = request.getRequestURI();
                    if (uri != null && uri.contains("/oauth2/authorization/")) {
                        String referer = request.getHeader("Referer");
                        if (referer != null && !referer.isBlank()) {
                            try {
                                java.net.URI u = new java.net.URI(referer);
                                String origin = u.getScheme() + "://" + u.getHost() +
                                        (u.getPort() > 0 && u.getPort() != 80 && u.getPort() != 443 ? ":" + u.getPort() : "");
                                request.getSession(true).setAttribute("OAUTH2_REDIRECT_ORIGIN", origin);
                            } catch (Exception ignored) {}
                        }
                    }
                    filterChain.doFilter(request, response);
                }
            }, org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/oauth2/**",
                    "/login/**",
                    "/error",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/api/v1/payments/webhook",
                    "/api/v1/payments/webhook/**",
                    "/api/v1/public/certificates/**",
                    "/api/v1/public/knowledge/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/payments/config").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/marketplace/courses/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/courses/*/discussions", "/api/v1/courses/*/discussions/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/courses/*/announcements", "/api/v1/courses/*/announcements/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/membership/plans").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/public/certificates/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/public/media/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/teacher-applications/documents/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                .requestMatchers("/api/v1/auth/me").authenticated()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/teacher/questions/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/api/v1/teacher/subjects/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/api/v1/teacher/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/v1/student/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"message\":\"Phiên đăng nhập đã hết hạn hoặc bạn chưa đăng nhập. Vui lòng đăng nhập lại để tiếp tục sử dụng.\",\"success\":false}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"message\":\"Bạn không có quyền truy cập chức năng này.\",\"success\":false}");
                })
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2LoginSuccessHandler)
                .failureUrl(frontendUrl + "/login?error=oauth2_failure")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                    .oidcUserService(customOidcUserService)
                )
            )
            .logout(logout -> logout
                .logoutUrl("/api/v1/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"message\":\"Đăng xuất thành công.\",\"success\":true}");
                })
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = new java.util.ArrayList<>(List.of(
            "http://localhost:3000",
            "http://localhost:3001",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001",
            "http://localhost:5173",
            "https://*.vercel.app",
            "https://*.onrender.com",
            "https://nqdlms.online",
            "https://www.nqdlms.online",
            "https://*.nqdlms.online",
            "http://nqdlms.online",
            "http://www.nqdlms.online"
        ));
        if (frontendUrl != null && !frontendUrl.isBlank() && !origins.contains(frontendUrl)) {
            origins.add(frontendUrl);
        }
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
