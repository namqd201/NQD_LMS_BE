package com.nqd.nqd_lms_be.service.r2;

import com.nqd.nqd_lms_be.config.r2.R2Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class R2StorageServiceImpl implements R2StorageService {

    private final S3Client s3Client;
    private final R2Properties r2Properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    @Override
    public String uploadStream(String key, InputStream inputStream, long contentLength, String contentType) {
        if (!r2Properties.isConfigured()) {
            log.warn("Cloudflare R2 is not configured. Skipping upload for key={}", key);
            return "";
        }

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(key)
                    .contentType(contentType != null ? contentType : "video/mp4")
                    .build();

            RequestBody requestBody = contentLength > 0
                    ? RequestBody.fromInputStream(inputStream, contentLength)
                    : RequestBody.fromBytes(inputStream.readAllBytes());

            s3Client.putObject(putRequest, requestBody);
            String publicUrl = getPublicUrl(key);
            log.info("Successfully uploaded object to Cloudflare R2: key={}, url={}", key, publicUrl);
            return publicUrl;
        } catch (Exception e) {
            log.error("Failed to upload stream to Cloudflare R2 for key={}", key, e);
            throw new RuntimeException("Lỗi tải tệp lên Cloudflare R2: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadFromUrl(String sourceUrl, String targetKey, String contentType) {
        if (!r2Properties.isConfigured()) {
            log.warn("Cloudflare R2 is not configured. Cannot stream from sourceUrl={}", sourceUrl);
            return sourceUrl; // Fallback to original URL
        }

        log.info("Streaming video from sourceUrl to Cloudflare R2 key={}", targetKey);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(sourceUrl))
                    .GET()
                    .timeout(Duration.ofMinutes(10))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                log.error("Failed to download source video from {}: HTTP {}", sourceUrl, response.statusCode());
                return sourceUrl;
            }

            long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            String detectedType = response.headers().firstValue("Content-Type").orElse(contentType);

            try (InputStream is = response.body()) {
                return uploadStream(targetKey, is, contentLength, detectedType);
            }
        } catch (Exception e) {
            log.error("Exception while streaming video from {} to Cloudflare R2", sourceUrl, e);
            return sourceUrl; // Graceful fallback to source URL
        }
    }

    @Override
    public void deleteFile(String key) {
        if (!r2Properties.isConfigured() || key == null || key.isBlank()) {
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Deleted object from Cloudflare R2: key={}", key);
        } catch (Exception e) {
            log.error("Failed to delete object from Cloudflare R2: key={}", key, e);
        }
    }

    @Override
    public String getPublicUrl(String key) {
        String base = r2Properties.getPublicUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        return base + "/" + cleanKey;
    }
}
