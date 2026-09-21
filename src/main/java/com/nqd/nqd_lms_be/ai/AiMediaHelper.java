package com.nqd.nqd_lms_be.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class AiMediaHelper {

    private static final String MEDIA_UPLOAD_DIR = "uploads/media";
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024; // 8MB max per image
    private static final int MAX_IMAGES_COUNT = 5;

    private static final Pattern HTML_IMG_SRC_PATTERN = Pattern.compile(
            "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MD_IMG_PATTERN = Pattern.compile(
            "!\\[.*?\\]\\((https?://[^\\s\\)]+|/[^\\s\\)]+)\\)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DIRECT_URL_PATTERN = Pattern.compile(
            "(https?://[^\\s]+(?:/api/v1/public/media/[^\\s\"'<>]+|\\.(?:png|jpe?g|webp|gif)))",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern RELATIVE_MEDIA_URL_PATTERN = Pattern.compile(
            "(/api/v1/public/media/[a-zA-Z0-9._-]+)",
            Pattern.CASE_INSENSITIVE
    );

    private final HttpClient httpClient;

    public AiMediaHelper() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    public static class ExtractedImage {
        private final String mimeType;
        private final String base64Data;
        private final String sourceHint;

        public ExtractedImage(String mimeType, String base64Data, String sourceHint) {
            this.mimeType = mimeType;
            this.base64Data = base64Data;
            this.sourceHint = sourceHint;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getBase64Data() {
            return base64Data;
        }

        public String getSourceHint() {
            return sourceHint;
        }

        public Map<String, Object> toGeminiPart() {
            return Map.of("inline_data", Map.of(
                    "mime_type", mimeType,
                    "data", base64Data
            ));
        }
    }

    /**
     * Extract images from HTML, Markdown, or raw URLs embedded in text.
     * Returns a list of ExtractedImage ready for Gemini inline_data.
     */
    public List<ExtractedImage> extractImages(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        Set<String> imageSources = new LinkedHashSet<>();

        // 1. Match HTML <img src="...">
        Matcher htmlMatcher = HTML_IMG_SRC_PATTERN.matcher(text);
        while (htmlMatcher.find()) {
            String src = htmlMatcher.group(1).trim();
            if (!src.isEmpty()) imageSources.add(src);
        }

        // 2. Match Markdown ![alt](url)
        Matcher mdMatcher = MD_IMG_PATTERN.matcher(text);
        while (mdMatcher.find()) {
            String url = mdMatcher.group(1).trim();
            if (!url.isEmpty()) imageSources.add(url);
        }

        // 3. Match relative API URLs: /api/v1/public/media/...
        Matcher relMatcher = RELATIVE_MEDIA_URL_PATTERN.matcher(text);
        while (relMatcher.find()) {
            String rel = relMatcher.group(1).trim();
            if (!rel.isEmpty()) imageSources.add(rel);
        }

        // 4. Match full URLs
        Matcher directMatcher = DIRECT_URL_PATTERN.matcher(text);
        while (directMatcher.find()) {
            String url = directMatcher.group(1).trim();
            if (!url.isEmpty()) imageSources.add(url);
        }

        List<ExtractedImage> results = new ArrayList<>();
        int count = 0;
        for (String source : imageSources) {
            if (count >= MAX_IMAGES_COUNT) break;
            ExtractedImage img = resolveImage(source);
            if (img != null) {
                results.add(img);
                count++;
            }
        }

        return results;
    }

    /**
     * Replaces <img> tags and markdown image tags with clean labels like [Hình ảnh 1], [Hình ảnh 2]
     * so that prompt text remains clean without losing spatial context.
     */
    public String formatTextWithImagePlaceholders(String text) {
        if (text == null || text.isBlank()) return "";

        String cleaned = text;

        // Replace <img> tags with [Hình ảnh]
        cleaned = HTML_IMG_SRC_PATTERN.matcher(cleaned).replaceAll(" [Hình ảnh đính kèm] ");

        // Replace markdown images ![...](...) with [Hình ảnh bài làm]
        cleaned = MD_IMG_PATTERN.matcher(cleaned).replaceAll(" [Hình ảnh bài làm] ");

        // Remove redundant HTML tags
        cleaned = cleaned.replaceAll("<[^>]*>", " ");

        // Clean extra whitespace
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        return cleaned;
    }

    private ExtractedImage resolveImage(String source) {
        if (source == null || source.isBlank()) return null;

        String trimmed = source.trim();

        // Case A: Data URI (e.g. data:image/jpeg;base64,...)
        if (trimmed.startsWith("data:")) {
            try {
                int commaIdx = trimmed.indexOf(',');
                if (commaIdx > 0) {
                    String header = trimmed.substring(5, commaIdx);
                    String base64 = trimmed.substring(commaIdx + 1).replaceAll("\\s+", "");
                    String mimeType = "image/png";
                    if (header.contains(";")) {
                        mimeType = header.substring(0, header.indexOf(";"));
                    } else if (!header.isBlank()) {
                        mimeType = header;
                    }
                    return new ExtractedImage(mimeType, base64, "Data URI");
                }
            } catch (Exception e) {
                log.warn("Failed to parse Data URI image: {}", e.getMessage());
            }
            return null;
        }

        // Case B: Local storage (/api/v1/public/media/{filename} or uploads/media/{filename})
        String filename = extractFilenameFromMediaUrl(trimmed);
        if (filename != null) {
            Path filePath = Paths.get(MEDIA_UPLOAD_DIR, filename).normalize();
            if (Files.exists(filePath) && Files.isReadable(filePath)) {
                try {
                    byte[] bytes = Files.readAllBytes(filePath);
                    if (bytes.length > MAX_IMAGE_BYTES) {
                        log.warn("Image {} exceeds size limit ({} bytes), skipping", filename, bytes.length);
                        return null;
                    }
                    String mimeType = detectMimeType(filename, bytes);
                    String base64 = Base64.getEncoder().encodeToString(bytes);
                    log.info("Loaded local image: {} ({} bytes, mime: {})", filename, bytes.length, mimeType);
                    return new ExtractedImage(mimeType, base64, filename);
                } catch (IOException e) {
                    log.warn("Failed to read local image file {}: {}", filePath, e.getMessage());
                }
            }
        }

        // Case C: Remote HTTP / HTTPS URL
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            // If it's pointing to localhost's own media endpoint, try resolving locally first
            if (trimmed.contains("/api/v1/public/media/")) {
                String localName = extractFilenameFromMediaUrl(trimmed);
                if (localName != null) {
                    Path localPath = Paths.get(MEDIA_UPLOAD_DIR, localName).normalize();
                    if (Files.exists(localPath)) {
                        try {
                            byte[] bytes = Files.readAllBytes(localPath);
                            String mimeType = detectMimeType(localName, bytes);
                            return new ExtractedImage(mimeType, Base64.getEncoder().encodeToString(bytes), localName);
                        } catch (IOException ignored) {}
                    }
                }
            }

            // Otherwise download with timeout
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(trimmed))
                        .timeout(Duration.ofSeconds(6))
                        .GET()
                        .build();
                HttpResponse<byte[]> res = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (res.statusCode() == 200 && res.body() != null && res.body().length > 0) {
                    byte[] bytes = res.body();
                    if (bytes.length <= MAX_IMAGE_BYTES) {
                        String contentType = res.headers().firstValue("Content-Type").orElse("image/jpeg");
                        if (contentType.contains(";")) {
                            contentType = contentType.substring(0, contentType.indexOf(";")).trim();
                        }
                        return new ExtractedImage(contentType, Base64.getEncoder().encodeToString(bytes), trimmed);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch remote image {}: {}", trimmed, e.getMessage());
            }
        }

        return null;
    }

    private String extractFilenameFromMediaUrl(String url) {
        if (url.contains("/api/v1/public/media/")) {
            int idx = url.indexOf("/api/v1/public/media/");
            String sub = url.substring(idx + "/api/v1/public/media/".length());
            int queryIdx = sub.indexOf('?');
            if (queryIdx > 0) sub = sub.substring(0, queryIdx);
            return sub.replaceAll("[^a-zA-Z0-9._-]", "");
        }
        if (url.startsWith("uploads/media/")) {
            return url.substring("uploads/media/".length()).replaceAll("[^a-zA-Z0-9._-]", "");
        }
        return null;
    }

    private String detectMimeType(String filename, byte[] bytes) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";

        // Magic numbers check
        if (bytes != null && bytes.length >= 4) {
            if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
                return "image/png";
            }
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) {
                return "image/jpeg";
            }
        }
        return "image/jpeg";
    }
}
