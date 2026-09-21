package com.nqd.nqd_lms_be.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SameSiteCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(response) {
            @Override
            public void addHeader(String name, String value) {
                if ("Set-Cookie".equalsIgnoreCase(name) && value != null) {
                    value = formatCookie(value);
                }
                super.addHeader(name, value);
            }

            @Override
            public void setHeader(String name, String value) {
                if ("Set-Cookie".equalsIgnoreCase(name) && value != null) {
                    value = formatCookie(value);
                }
                super.setHeader(name, value);
            }

            private String formatCookie(String cookie) {
                if (cookie.contains("JSESSIONID")) {
                    if (cookie.matches("(?i).*SameSite=\\w+.*")) {
                        cookie = cookie.replaceAll("(?i)SameSite=\\w+", "SameSite=None");
                    } else {
                        cookie = cookie + "; SameSite=None";
                    }
                    if (!cookie.toLowerCase().contains("secure")) {
                        cookie = cookie + "; Secure";
                    }
                    if (!cookie.toLowerCase().contains("partitioned")) {
                        cookie = cookie + "; Partitioned";
                    }
                }
                return cookie;
            }
        };

        filterChain.doFilter(request, responseWrapper);
    }
}
