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

            // STEP 2: Stream Video Content to YouTube
            HttpRequest.Builder uploadBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(uploadLocation))
                    .header("Content-Type", "video/mp4")
                    .timeout(Duration.ofMinutes(45));

            if (contentLength > 0) {
                uploadBuilder.header("Content-Length", String.valueOf(contentLength));
                uploadBuilder.PUT(HttpRequest.BodyPublishers.ofInputStream(() -> inputStream));
            } else {
                byte[] bytes = inputStream.readAllBytes();
                uploadBuilder.header("Content-Length", String.valueOf(bytes.length));
                uploadBuilder.PUT(HttpRequest.BodyPublishers.ofByteArray(bytes));
            }

            HttpResponse<String> uploadResponse = httpClient.send(uploadBuilder.build(), HttpResponse.BodyHandlers.ofString());
            log.info("YouTube upload finished with status: {}", uploadResponse.statusCode());

            if (uploadResponse.statusCode() == 200 || uploadResponse.statusCode() == 201) {
                JsonNode resultNode = objectMapper.readTree(uploadResponse.body());
                String videoId = resultNode.path("id").asText();
                if (videoId != null && !videoId.isBlank()) {
                    String youtubeUrl = "https://www.youtube.com/watch?v=" + videoId;
                    log.info("Successfully uploaded class recording to YouTube: {}", youtubeUrl);
                    return youtubeUrl;
                }
            } else {
                log.error("YouTube upload failed: status={}, body={}", uploadResponse.statusCode(), uploadResponse.body());
            }
        } catch (Exception e) {
            log.error("Exception uploading video stream to YouTube", e);
        }

        return "";
    }
}
