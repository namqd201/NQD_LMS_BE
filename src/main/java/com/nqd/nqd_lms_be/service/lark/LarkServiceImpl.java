package com.nqd.nqd_lms_be.service.lark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.lark.LarkProperties;
import com.nqd.nqd_lms_be.dto.lark.LarkMeetingReservationDto;
import com.nqd.nqd_lms_be.dto.lark.LarkRecordingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LarkServiceImpl implements LarkService {

    private final LarkProperties larkProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String cachedToken = null;
    private long tokenExpiresAt = 0;

    @Override
    public synchronized String getTenantAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiresAt) {
            return cachedToken;
        }

        try {
            Map<String, String> requestBody = Map.of(
                    "app_id", larkProperties.getAppId(),
                    "app_secret", larkProperties.getAppSecret()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(larkProperties.getBaseUrl() + "/open-apis/auth/v3/tenant_access_token/internal"))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.path("code").asInt() == 0) {
                    cachedToken = root.path("tenant_access_token").asText();
                    int expireSeconds = root.path("expire").asInt(7200);
                    tokenExpiresAt = now + ((expireSeconds - 300L) * 1000L);
                    log.info("Lark tenant_access_token refreshed successfully, expires in {}s", expireSeconds);
                    return cachedToken;
                } else {
                    log.error("Failed to obtain Lark token: code={}, msg={}", root.path("code"), root.path("msg"));
                }
            } else {
                log.error("Lark token request HTTP error: status={}, body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Exception while fetching Lark tenant access token", e);
        }

        return cachedToken != null ? cachedToken : "";
    }

    @Override
    public LarkMeetingReservationDto createMeetingReservation(String topic, int durationDays) {
        if (!larkProperties.isEnabled()) {
            log.warn("Lark integration is disabled in configuration.");
            return null;
        }

        String token = getTenantAccessToken();
        if (token == null || token.isBlank()) {
            log.error("Cannot create meeting reservation: Lark access token is empty.");
            return null;
        }

        try {
            long nowSec = System.currentTimeMillis() / 1000L;
            // Lark reservation allows up to 30 days
            long safeDurationDays = Math.min(durationDays, 30L);
            long endSec = nowSec + (safeDurationDays * 86400L);

            Map<String, Object> meetingSettings = new HashMap<>();
            meetingSettings.put("topic", topic != null && !topic.isBlank() ? topic : "Phòng học trực tuyến NQD-LMS");

            Map<String, Object> body = new HashMap<>();
            body.put("end_time", String.valueOf(endSec));
            body.put("meeting_settings", meetingSettings);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(larkProperties.getBaseUrl() + "/open-apis/vc/v1/reserves/apply?user_id_type=open_id"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Lark create reserve response: status={}, body={}", response.statusCode(), response.body());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.path("code").asInt() == 0) {
                    JsonNode reserveNode = root.path("data").path("reserve");
                    String meetingUrl = reserveNode.path("url").asText(reserveNode.path("meeting_url").asText());
                    String meetingId = reserveNode.path("id").asText(reserveNode.path("meeting_no").asText());
                    String passcode = reserveNode.path("password").asText();

                    log.info("Successfully created Lark meeting reservation: url={}, id={}", meetingUrl, meetingId);
                    return LarkMeetingReservationDto.builder()
                            .meetingUrl(meetingUrl)
                            .meetingId(meetingId)
                            .reserveId(reserveNode.path("id").asText())
                            .passcode(passcode)
                            .topic(topic)
                            .endTime(endSec)
                            .build();
                } else {
                    log.warn("Lark reserve API returned code {}: {}", root.path("code"), root.path("msg"));
                }
            }
        } catch (Exception e) {
            log.error("Exception while calling Lark VC reserves API", e);
        }

        return null;
    }

    @Override
    public LarkRecordingDto getMeetingRecording(String meetingId) {
        if (!larkProperties.isEnabled() || meetingId == null || meetingId.isBlank()) {
            return null;
        }

        String token = getTenantAccessToken();
        if (token == null || token.isBlank()) return null;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(larkProperties.getBaseUrl() + "/open-apis/vc/v1/meetings/" + meetingId + "/recording"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.path("code").asInt() == 0) {
                    JsonNode recordingNode = root.path("data").path("recording");
                    String url = recordingNode.path("url").asText();
                    long duration = recordingNode.path("duration").asLong(0L);

                    return LarkRecordingDto.builder()
                            .meetingId(meetingId)
                            .url(url)
                            .durationMs(duration)
                            .build();
                } else {
                    log.warn("Failed to get recording: code={}, msg={}", root.path("code"), root.path("msg"));
                }
            }
        } catch (Exception e) {
            log.error("Exception fetching Lark meeting recording for meetingId={}", meetingId, e);
        }

        return null;
    }

    @Override
    public void deleteMeetingRecording(String meetingId) {
        if (!larkProperties.isEnabled() || meetingId == null || meetingId.isBlank()) {
            return;
        }

        String token = getTenantAccessToken();
        if (token == null || token.isBlank()) return;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(larkProperties.getBaseUrl() + "/open-apis/vc/v1/meetings/" + meetingId + "/recording"))
                    .header("Authorization", "Bearer " + token)
                    .DELETE()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Lark delete recording response for meetingId={}: status={}", meetingId, response.statusCode());
        } catch (Exception e) {
            log.error("Exception deleting Lark meeting recording for meetingId={}", meetingId, e);
        }
    }
}
