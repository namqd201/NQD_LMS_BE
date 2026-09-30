package com.nqd.nqd_lms_be.service.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.google.GoogleProperties;
import com.nqd.nqd_lms_be.dto.google.GoogleMeetReservationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleMeetServiceImpl implements GoogleMeetService {

    private final GoogleOAuthService googleOAuthService;
    private final GoogleProperties googleProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    @Override
    public GoogleMeetReservationDto createClassroomMeeting(String classroomName, String description) {
        if (!googleProperties.getMeet().isEnabled() || !googleOAuthService.isConfigured()) {
            log.warn("Google Meet integration is disabled or credentials not configured.");
            return null;
        }

        String accessToken = googleOAuthService.getFreshAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            log.error("Unable to get valid Google access token for creating Meet room.");
            return null;
        }

        try {
            Instant now = Instant.now();
            Instant oneHourLater = now.plus(1, ChronoUnit.HOURS);

            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("summary", "Lớp học: " + classroomName);
            bodyMap.put("description", (description != null && !description.isBlank()) 
                    ? description 
                    : "Phòng học trực tuyến Google Meet cố định cho lớp: " + classroomName);

            Map<String, String> startMap = new HashMap<>();
            startMap.put("dateTime", now.toString());
            bodyMap.put("start", startMap);

            Map<String, String> endMap = new HashMap<>();
            endMap.put("dateTime", oneHourLater.toString());
            bodyMap.put("end", endMap);

            // Weekly recurrence so the meeting link stays active permanently
            bodyMap.put("recurrence", List.of("RRULE:FREQ=WEEKLY;COUNT=100"));

            // Request Google Meet conference data
            Map<String, Object> createReq = new HashMap<>();
            createReq.put("requestId", UUID.randomUUID().toString());
            createReq.put("conferenceSolutionKey", Map.of("type", "hangoutsMeet"));

            Map<String, Object> conferenceData = new HashMap<>();
            conferenceData.put("createRequest", createReq);
            bodyMap.put("conferenceData", conferenceData);

            String jsonPayload = objectMapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/calendar/v3/calendars/primary/events?conferenceDataVersion=1"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JsonNode root = objectMapper.readTree(response.body());
                String eventId = root.path("id").asText();
                String hangoutLink = root.path("hangoutLink").asText();

                // If hangoutLink is empty, try entryPoints
                if (hangoutLink == null || hangoutLink.isBlank()) {
                    JsonNode entryPoints = root.path("conferenceData").path("entryPoints");
                    if (entryPoints.isArray() && entryPoints.size() > 0) {
                        hangoutLink = entryPoints.get(0).path("uri").asText();
                    }
                }

                String meetingCode = null;
                if (hangoutLink != null && hangoutLink.contains("meet.google.com/")) {
                    meetingCode = hangoutLink.substring(hangoutLink.lastIndexOf('/') + 1);
                }

                log.info("Successfully provisioned Google Meet room: url={}, eventId={}", hangoutLink, eventId);

                return GoogleMeetReservationDto.builder()
                        .meetingUrl(hangoutLink)
                        .eventId(eventId)
                        .meetingCode(meetingCode)
                        .summary("Lớp học: " + classroomName)
                        .build();
            } else {
                log.error("Google Calendar API returned error status {}: body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to provision Google Meet room for classroom {}", classroomName, e);
        }

        return null;
    }

    @Override
    public void deleteMeetingEvent(String eventId) {
        if (eventId == null || eventId.isBlank() || !googleOAuthService.isConfigured()) return;

        try {
            String accessToken = googleOAuthService.getFreshAccessToken();
            if (accessToken == null || accessToken.isBlank()) return;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/calendar/v3/calendars/primary/events/" + eventId))
                    .header("Authorization", "Bearer " + accessToken)
                    .DELETE()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Deleted Google Calendar event {}: status={}", eventId, response.statusCode());
        } catch (Exception e) {
            log.warn("Failed to delete Google Calendar event {}: {}", eventId, e.getMessage());
        }
    }
}
