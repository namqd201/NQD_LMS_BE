package com.nqd.nqd_lms_be.service.onehundredms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.onehundredms.OneHundredMsProperties;
import com.nqd.nqd_lms_be.dto.onehundredms.OneHundredMsPeerDto;
import com.nqd.nqd_lms_be.dto.onehundredms.OneHundredMsRecordingAssetDto;
import com.nqd.nqd_lms_be.dto.onehundredms.OneHundredMsRoomDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OneHundredMsServiceImpl implements OneHundredMsService {

    private final OneHundredMsProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    public boolean isConfigured() {
        return properties.isEnabled()
                && properties.getAppAccessKey() != null && !properties.getAppAccessKey().isBlank()
                && properties.getAppSecret() != null && !properties.getAppSecret().isBlank();
    }

    public String generateManagementToken() {
        if (!isConfigured()) {
            throw new IllegalStateException("100ms credentials not configured.");
        }
        long now = System.currentTimeMillis();
        Map<String, Object> payload = new HashMap<>();
        payload.put("access_key", properties.getAppAccessKey());
        payload.put("type", "management");
        payload.put("version", 2);

        SecretKey key = Keys.hmacShaKeyFor(properties.getAppSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claims(payload)
                .id(UUID.randomUUID().toString())
                .expiration(new Date(now + 24 * 3600 * 1000L))
                .issuedAt(new Date(now - 60 * 1000L))
                .notBefore(new Date(now - 60 * 1000L))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public OneHundredMsRoomDto createClassroomMeeting(String classroomName, String description) {
        if (!isConfigured()) {
            log.warn("100ms is not enabled or credentials missing.");
            return null;
        }

        try {
            String token = generateManagementToken();

            String sanitized = (classroomName != null ? classroomName : "class")
                    .toLowerCase()
                    .replaceAll("[^a-z0-9]", "-")
                    .replaceAll("-+", "-");
            if (sanitized.startsWith("-")) sanitized = sanitized.substring(1);
            if (sanitized.length() > 25) sanitized = sanitized.substring(0, 25);
            String roomName = (sanitized.isBlank() ? "class" : sanitized) + "-" + System.currentTimeMillis();

            Map<String, Object> roomReq = new HashMap<>();
            roomReq.put("name", roomName);
            roomReq.put("description", description != null ? description : "Phòng học 100ms lớp " + classroomName);
            if (properties.getTemplateId() != null && !properties.getTemplateId().isBlank()) {
                roomReq.put("template_id", properties.getTemplateId());
            }

            HttpRequest createRoomReq = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getApiBaseUrl() + "/rooms"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(roomReq)))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> roomResp = httpClient.send(createRoomReq, HttpResponse.BodyHandlers.ofString());
            if (roomResp.statusCode() < 200 || roomResp.statusCode() >= 300) {
                log.error("100ms create room failed: status={}, body={}", roomResp.statusCode(), roomResp.body());
                throw new RuntimeException("Lỗi tạo phòng 100ms (HTTP " + roomResp.statusCode() + "): " + roomResp.body());
            }

            JsonNode roomNode = objectMapper.readTree(roomResp.body());
            String roomId = roomNode.path("id").asText();

            // Create or fetch room codes for this room
            HttpRequest codeReq = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getApiBaseUrl() + "/room-codes/room/" + roomId))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> codeResp = httpClient.send(codeReq, HttpResponse.BodyHandlers.ofString());
            JsonNode codeNode = null;
            if (codeResp.statusCode() >= 200 && codeResp.statusCode() < 300) {
                codeNode = objectMapper.readTree(codeResp.body());
            } else {
                // If POST failed (e.g. already exists), try GET
                HttpRequest getCodeReq = HttpRequest.newBuilder()
                        .uri(URI.create(properties.getApiBaseUrl() + "/room-codes/room/" + roomId))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .timeout(Duration.ofSeconds(15))
                        .build();
                HttpResponse<String> getCodeResp = httpClient.send(getCodeReq, HttpResponse.BodyHandlers.ofString());
                if (getCodeResp.statusCode() >= 200 && getCodeResp.statusCode() < 300) {
                    codeNode = objectMapper.readTree(getCodeResp.body());
                }
            }

            String hostCode = null;
            String guestCode = null;

            if (codeNode != null && codeNode.has("data") && codeNode.path("data").isArray()) {
                for (JsonNode item : codeNode.path("data")) {
                    String role = item.path("role").asText("");
                    String code = item.path("code").asText("");
                    if ("host".equalsIgnoreCase(role)) {
                        hostCode = code;
                    } else if ("guest".equalsIgnoreCase(role)) {
                        guestCode = code;
                    }
                }
            }

            String subdomain = properties.getSubdomain() != null ? properties.getSubdomain() : "app";
            String hostUrl = (hostCode != null && !hostCode.isBlank())
                    ? String.format("https://%s.app.100ms.live/meeting/%s", subdomain, hostCode)
                    : null;
            String guestUrl = (guestCode != null && !guestCode.isBlank())
                    ? String.format("https://%s.app.100ms.live/meeting/%s", subdomain, guestCode)
                    : hostUrl;

            log.info("Provisioned 100ms room: roomId={}, hostCode={}, guestCode={}", roomId, hostCode, guestCode);

            return OneHundredMsRoomDto.builder()
                    .roomId(roomId)
                    .roomName(roomName)
                    .templateId(properties.getTemplateId())
                    .hostCode(hostCode)
                    .guestCode(guestCode)
                    .hostMeetingUrl(hostUrl)
                    .guestMeetingUrl(guestUrl)
                    .enabled(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to provision 100ms room for {}", classroomName, e);
            throw new RuntimeException("Không thể tạo phòng học 100ms: " + e.getMessage(), e);
        }
    }

    @Override
    public void disableMeeting(String roomId) {
        if (!isConfigured() || roomId == null || roomId.isBlank()) return;

        try {
            String token = generateManagementToken();
            Map<String, Object> req = Map.of("enabled", false);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getApiBaseUrl() + "/rooms/" + roomId))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(req)))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            log.info("Disabled 100ms room {}", roomId);
        } catch (Exception e) {
            log.warn("Failed to disable 100ms room {}: {}", roomId, e.getMessage());
        }
    }

    @Override
    public List<OneHundredMsRecordingAssetDto> getCompletedRecordingAssets(String roomId) {
        if (!isConfigured() || roomId == null || roomId.isBlank()) return Collections.emptyList();

        try {
            String token = generateManagementToken();
            String uriStr = properties.getApiBaseUrl() + "/recording-assets?room_id=" + roomId + "&status=completed";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(uriStr))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                log.warn("Failed to query recording assets for room {}: status={}", roomId, resp.statusCode());
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(resp.body());
            List<OneHundredMsRecordingAssetDto> list = new ArrayList<>();
            if (root.has("data") && root.path("data").isArray()) {
                for (JsonNode item : root.path("data")) {
                    list.add(OneHundredMsRecordingAssetDto.builder()
                            .id(item.path("id").asText())
                            .roomId(item.path("room_id").asText())
                            .sessionId(item.path("session_id").asText())
                            .status(item.path("status").asText())
                            .type(item.path("type").asText())
                            .duration(item.path("duration").asLong(0))
                            .createdAt(item.path("created_at").asText())
                            .build());
                }
            }
            return list;
        } catch (Exception e) {
            log.warn("Error getting recording assets for room {}: {}", roomId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public String getPresignedDownloadUrl(String assetId) {
        if (!isConfigured() || assetId == null || assetId.isBlank()) return null;

        try {
            String token = generateManagementToken();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getApiBaseUrl() + "/recording-assets/" + assetId + "/presigned-url"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(resp.body());
                return node.path("url").asText(null);
            }
            log.warn("Failed to get presigned URL for asset {}: status={}", assetId, resp.statusCode());
            return null;
        } catch (Exception e) {
            log.warn("Error getting presigned URL for asset {}: {}", assetId, e.getMessage());
            return null;
        }
    }

    @Override
    public List<OneHundredMsPeerDto> getActivePeers(String roomId) {
        if (!isConfigured() || roomId == null || roomId.isBlank()) {
            return Collections.emptyList();
        }

        try {
            String token = generateManagementToken();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getApiBaseUrl() + "/active-rooms/" + roomId + "/peers"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 404) {
                // Room is not currently active
                return Collections.emptyList();
            }
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                log.warn("Failed to query active peers for room {}: status={}, body={}", roomId, resp.statusCode(), resp.body());
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(resp.body());
            List<OneHundredMsPeerDto> peers = new ArrayList<>();

            JsonNode peersNode = root.path("peers");
            if (peersNode.isMissingNode() || peersNode.isNull()) {
                peersNode = root.path("data");
            }

            if (peersNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = peersNode.fields();
                while (fields.hasNext()) {
                    JsonNode node = fields.next().getValue();
                    peers.add(parsePeerNode(node));
                }
            } else if (peersNode.isArray()) {
                for (JsonNode node : peersNode) {
                    peers.add(parsePeerNode(node));
                }
            }

            return peers;
        } catch (Exception e) {
            log.warn("Error getting active peers for 100ms room {}: {}", roomId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isRoomActive(String roomId) {
        if (!isConfigured() || roomId == null || roomId.isBlank()) {
            return false;
        }
        try {
            String token = generateManagementToken();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getApiBaseUrl() + "/active-rooms/" + roomId))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .timeout(Duration.ofSeconds(8))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Error checking active room status for {}: {}", roomId, e.getMessage());
            return false;
        }
    }

    private OneHundredMsPeerDto parsePeerNode(JsonNode node) {
        String id = node.path("id").isMissingNode() ? node.path("peer_id").asText("") : node.path("id").asText();
        String name = node.path("name").isMissingNode() ? node.path("user_name").asText("") : node.path("name").asText();
        String role = node.path("role").asText("");
        String userId = node.path("user_id").asText("");
        String joinedAt = node.path("joined_at").asText("");

        return OneHundredMsPeerDto.builder()
                .id(id)
                .name(name)
                .role(role)
                .userId(userId)
                .joinedAt(joinedAt)
                .build();
    }
}

