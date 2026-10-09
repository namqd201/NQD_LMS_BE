package com.nqd.nqd_lms_be.service.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nqd.nqd_lms_be.config.youtube.YouTubeProperties;
import com.nqd.nqd_lms_be.service.google.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class YouTubeUploadServiceImpl implements YouTubeUploadService {

    private final YouTubeProperties youTubeProperties;
    private final GoogleOAuthService googleOAuthService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    @Override
    public boolean isConfigured() {
        return (youTubeProperties != null && youTubeProperties.isConfigured()) 
                || (googleOAuthService != null && googleOAuthService.isConfigured());
    }

    @Override
    public String getFreshAccessToken() {
        if (googleOAuthService != null && googleOAuthService.isConfigured()) {
            return googleOAuthService.getFreshAccessToken();
        }
        return "";
    }

    @Override
    public String uploadVideoFromUrl(String sourceUrl, String title, String description, List<String> tags) {
        if (!isConfigured()) {
            log.warn("YouTube service not configured. Skipping upload for title: {}", title);
            return "";
        }

        try {
            HttpRequest downloadRequest = HttpRequest.newBuilder()
                    .uri(URI.create(sourceUrl))
                    .GET()
                    .timeout(Duration.ofMinutes(15))
                    .build();

            HttpResponse<InputStream> downloadResponse = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (downloadResponse.statusCode() != 200) {
                log.error("Failed to download video stream from URL {}: status={}", sourceUrl, downloadResponse.statusCode());
                return "";
            }

            long contentLength = downloadResponse.headers().firstValueAsLong("Content-Length").orElse(-1L);
            try (InputStream is = downloadResponse.body()) {
                return uploadVideoStream(is, contentLength, title, description, tags);
            }
        } catch (Exception e) {
            log.error("Exception downloading and uploading video from URL: {}", sourceUrl, e);
            return "";
        }
    }

    @Override
    public String uploadVideoStream(InputStream inputStream, long contentLength, String title, String description, List<String> tags) {
        if (!isConfigured()) {
            log.warn("YouTube service not configured. Skipping upload for title: {}", title);
            return "";
        }

        String accessToken = getFreshAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            log.error("Cannot upload to YouTube: empty access token.");
            return "";
        }

        try {
            // STEP 1: Initiate Resumable Upload Session
            log.info("Initiating YouTube resumable upload session for video: '{}'", title);

            Map<String, Object> snippet = new HashMap<>();
            snippet.put("title", title != null && !title.isBlank() ? title : "Buổi học online NQD-LMS");
            snippet.put("description", description != null ? description : "Bản ghi buổi học trực tuyến - Hệ thống NQD-LMS.");
            snippet.put("categoryId", "27"); // 27 = Education
            if (tags != null && !tags.isEmpty()) {
                snippet.put("tags", tags);
            }

            Map<String, Object> status = new HashMap<>();
            status.put("privacyStatus", "unlisted"); // Unlisted: Không công khai, chỉ ai có link mới xem được
            status.put("selfDeclaredMadeForKids", false);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("snippet", snippet);
            metadata.put("status", status);

            HttpRequest initRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("X-Upload-Content-Type", "video/mp4")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(metadata)))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> initResponse = httpClient.send(initRequest, HttpResponse.BodyHandlers.ofString());
            if (initResponse.statusCode() != 200) {
                log.error("Failed to initiate YouTube resumable upload: status={}, body={}", initResponse.statusCode(), initResponse.body());
                return "";
            }

            String uploadLocation = initResponse.headers().firstValue("Location").orElse(null);
            if (uploadLocation == null || uploadLocation.isBlank()) {
                log.error("YouTube did not return Location header for resumable upload.");
                return "";
            }

            log.info("Got YouTube upload session location. Streaming video data to YouTube...");

            // STEP 2: Stream Video Content to YouTube via HttpURLConnection to avoid restricted header errors and high memory usage
            java.net.URL url = URI.create(uploadLocation).toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "video/mp4");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(45 * 60 * 1000); // 45 minutes

            if (contentLength > 0) {
                conn.setFixedLengthStreamingMode(contentLength);
            } else {
                conn.setChunkedStreamingMode(1024 * 1024); // 1MB chunks
            }

            try (java.io.OutputStream os = conn.getOutputStream()) {
                inputStream.transferTo(os);
                os.flush();
            }

            int statusCode = conn.getResponseCode();
            log.info("YouTube upload finished with status: {}", statusCode);

            String responseBody = "";
            java.io.InputStream respStream = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (respStream != null) {
                try (respStream) {
                    responseBody = new String(respStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
            }

            if (statusCode == 200 || statusCode == 201) {
                JsonNode resultNode = objectMapper.readTree(responseBody);
                String videoId = resultNode.path("id").asText();
                if (videoId != null && !videoId.isBlank()) {
                    String youtubeUrl = "https://www.youtube.com/watch?v=" + videoId;
                    log.info("Successfully uploaded class recording to YouTube: {}", youtubeUrl);
                    return youtubeUrl;
                }
            } else {
                log.error("YouTube upload failed: status={}, body={}", statusCode, responseBody);
            }
        } catch (Exception e) {
            log.error("Exception uploading video stream to YouTube", e);
        }

        return "";
    }

    @Override
    public String getOrCreatePlaylist(String playlistTitle, String description, String existingPlaylistId) {
        if (!isConfigured()) {
            return "";
        }

        String accessToken = getFreshAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            return "";
        }

        // 1. If existingPlaylistId is provided, verify it exists
        if (existingPlaylistId != null && !existingPlaylistId.isBlank()) {
            try {
                HttpRequest verifyReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.googleapis.com/youtube/v3/playlists?part=id&id=" + existingPlaylistId))
                        .header("Authorization", "Bearer " + accessToken)
                        .GET()
                        .timeout(Duration.ofSeconds(15))
                        .build();

                HttpResponse<String> verifyResp = httpClient.send(verifyReq, HttpResponse.BodyHandlers.ofString());
                if (verifyResp.statusCode() == 200) {
                    JsonNode node = objectMapper.readTree(verifyResp.body());
                    if (node.path("items").isArray() && !node.path("items").isEmpty()) {
                        log.info("Using verified existing YouTube playlist: {}", existingPlaylistId);
                        return existingPlaylistId;
                    }
                }
            } catch (Exception e) {
                log.warn("Error verifying existing playlist {}: {}", existingPlaylistId, e.getMessage());
            }
        }

        // 2. Search existing playlists by title to avoid creating duplicates
        try {
            HttpRequest listReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/youtube/v3/playlists?part=snippet&mine=true&maxResults=50"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> listResp = httpClient.send(listReq, HttpResponse.BodyHandlers.ofString());
            if (listResp.statusCode() == 200) {
                JsonNode listNode = objectMapper.readTree(listResp.body());
                if (listNode.path("items").isArray()) {
                    for (JsonNode item : listNode.path("items")) {
                        String itemTitle = item.path("snippet").path("title").asText();
                        if (playlistTitle.equalsIgnoreCase(itemTitle)) {
                            String foundId = item.path("id").asText();
                            log.info("Found matching existing YouTube playlist '{}': {}", playlistTitle, foundId);
                            return foundId;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error searching YouTube playlists: {}", e.getMessage());
        }

        // 3. Create new unlisted playlist
        try {
            Map<String, Object> snippet = new HashMap<>();
            snippet.put("title", playlistTitle);
            snippet.put("description", description != null ? description : "Danh sách video buổi học NQD-LMS");

            Map<String, Object> status = new HashMap<>();
            status.put("privacyStatus", "unlisted");

            Map<String, Object> body = new HashMap<>();
            body.put("snippet", snippet);
            body.put("status", status);

            HttpRequest createReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/youtube/v3/playlists?part=snippet,status"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> createResp = httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());
            if (createResp.statusCode() == 200 || createResp.statusCode() == 201) {
                JsonNode resultNode = objectMapper.readTree(createResp.body());
                String createdId = resultNode.path("id").asText();
                log.info("Successfully created new YouTube playlist '{}': {}", playlistTitle, createdId);
                return createdId;
            } else {
                log.error("Failed to create YouTube playlist: status={}, body={}", createResp.statusCode(), createResp.body());
            }
        } catch (Exception e) {
            log.error("Exception creating YouTube playlist '{}'", playlistTitle, e);
        }

        return "";
    }

    @Override
    public boolean addVideoToPlaylist(String playlistId, String videoId) {
        if (!isConfigured() || playlistId == null || playlistId.isBlank() || videoId == null || videoId.isBlank()) {
            return false;
        }

        String accessToken = getFreshAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }

        try {
            // Check if already in playlist
            HttpRequest checkReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&playlistId=" + playlistId + "&videoId=" + videoId))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> checkResp = httpClient.send(checkReq, HttpResponse.BodyHandlers.ofString());
            if (checkResp.statusCode() == 200) {
                JsonNode checkNode = objectMapper.readTree(checkResp.body());
                if (checkNode.path("items").isArray() && !checkNode.path("items").isEmpty()) {
                    log.info("Video {} already in playlist {}. Skipping addition.", videoId, playlistId);
                    return true;
                }
            }

            // Insert into playlist
            Map<String, Object> resourceId = new HashMap<>();
            resourceId.put("kind", "youtube#video");
            resourceId.put("videoId", videoId);

            Map<String, Object> snippet = new HashMap<>();
            snippet.put("playlistId", playlistId);
            snippet.put("resourceId", resourceId);

            Map<String, Object> body = new HashMap<>();
            body.put("snippet", snippet);

            HttpRequest addReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/youtube/v3/playlistItems?part=snippet"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> addResp = httpClient.send(addReq, HttpResponse.BodyHandlers.ofString());
            if (addResp.statusCode() == 200 || addResp.statusCode() == 201) {
                log.info("Successfully added video {} to YouTube playlist {}", videoId, playlistId);
                return true;
            } else if (addResp.statusCode() == 409 || (addResp.body() != null && addResp.body().contains("videoAlreadyInPlaylist"))) {
                log.info("Video {} was already in playlist {}", videoId, playlistId);
                return true;
            } else {
                log.warn("Failed to add video {} to playlist {}: status={}, body={}", videoId, playlistId, addResp.statusCode(), addResp.body());
            }
        } catch (Exception e) {
            log.error("Exception adding video {} to playlist {}", videoId, playlistId, e);
        }

        return false;
    }
}
