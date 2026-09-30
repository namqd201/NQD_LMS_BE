package com.nqd.nqd_lms_be.service.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.google.GoogleProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    private final GoogleProperties googleProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private String cachedAccessToken = null;
    private long tokenExpiresAt = 0;

    @Override
    public boolean isConfigured() {
        return googleProperties.isConfigured();
    }

    @Override
    public synchronized String getFreshAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedAccessToken != null && now < tokenExpiresAt) {
            return cachedAccessToken;
        }

        if (!googleProperties.isConfigured()) {
            log.warn("Google Workspace credentials are not configured or incomplete.");
            return "";
        }

        try {
            String formBody = "client_id=" + URLEncoder.encode(googleProperties.getClientId().trim(), StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(googleProperties.getClientSecret().trim(), StandardCharsets.UTF_8)
                    + "&refresh_token=" + URLEncoder.encode(googleProperties.getRefreshToken().trim(), StandardCharsets.UTF_8)
                    + "&grant_type=refresh_token";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                cachedAccessToken = root.path("access_token").asText();
                int expiresIn = root.path("expires_in").asInt(3599);
                // Refresh 5 minutes before expiration
                tokenExpiresAt = now + ((expiresIn - 300L) * 1000L);
                log.info("Google OAuth access_token refreshed successfully, valid for {}s", expiresIn);
                return cachedAccessToken;
            } else {
                log.error("Failed to refresh Google access token: status={}, body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Exception while refreshing Google OAuth access token", e);
        }

        return cachedAccessToken != null ? cachedAccessToken : "";
    }
}
