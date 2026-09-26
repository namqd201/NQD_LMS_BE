package com.nqd.nqd_lms_be.ai.image;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AiImageServiceImpl implements AiImageService {

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private static final String UPLOAD_DIR = "uploads/media";
    private static final Random RANDOM = new Random();

    public AiImageServiceImpl(
            @Value("${lms.ai.gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String generateImage(String imagePrompt, String topic) {
        if (imagePrompt == null || imagePrompt.isBlank()) {
            return null;
        }

        String cleanedPrompt = sanitizePrompt(imagePrompt, topic);
        log.info("Generating educational diagram with prompt: '{}'", cleanedPrompt);

        // 1. Try Google Imagen if API key is present
        if (!apiKey.isBlank()) {
            try {
                byte[] imagenBytes = tryGoogleImagen(cleanedPrompt);
                if (imagenBytes != null && imagenBytes.length > 0) {
                    return saveImageFile(imagenBytes, "png");
                }
            } catch (Exception e) {
                log.warn("Google Imagen generation attempt failed: {}. Falling back to Pollinations AI...", e.getMessage());
            }
        }

        // 2. Pollinations AI (High quality, fast, specialized in illustrative educational diagrams)
        try {
            byte[] pollinationsBytes = tryPollinationsAi(cleanedPrompt);
            if (pollinationsBytes != null && pollinationsBytes.length > 0) {
                return saveImageFile(pollinationsBytes, "png");
            }
        } catch (Exception e) {
            log.warn("Pollinations AI image generation failed: {}", e.getMessage());
        }

        // 3. Graceful SVG diagram fallback if remote generators are offline
        try {
            byte[] fallbackSvg = generateFallbackSvg(imagePrompt, topic);
            return saveImageFile(fallbackSvg, "svg");
        } catch (Exception e) {
            log.error("Failed to generate fallback diagram: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public CompletableFuture<String> generateImageAsync(String imagePrompt, String topic) {
        return CompletableFuture.supplyAsync(() -> generateImage(imagePrompt, topic));
    }

    private String sanitizePrompt(String rawPrompt, String topic) {
        String p = rawPrompt.replaceAll("[\r\n]+", " ").trim();
        StringBuilder sb = new StringBuilder();
        sb.append(p);

        String lower = p.toLowerCase();
        if (!lower.contains("textbook") && !lower.contains("diagram") && !lower.contains("illustration")) {
            sb.append(", clear educational textbook diagram");
        }
        if (!lower.contains("white background")) {
            sb.append(", clean white background, high contrast, 2D vector style, minimal, no clutter");
        }
        if (topic != null && !topic.isBlank() && !lower.contains(topic.toLowerCase())) {
            sb.append(", subject: ").append(topic.trim());
        }

        return sb.toString();
    }

    private byte[] tryGoogleImagen(String prompt) {
        List<String> models = List.of(
                "imagen-3.0-generate-002",
                "imagen-3.0-fast-generate-001"
        );

        for (String model : models) {
            try {
                String url = String.format(
                        "https://generativelanguage.googleapis.com/v1beta/models/%s:predict?key=%s",
                        model, apiKey
                );

                Map<String, Object> payload = Map.of(
                        "instances", List.of(Map.of("prompt", prompt)),
                        "parameters", Map.of(
                                "sampleCount", 1,
                                "aspectRatio", "1:1",
                                "outputMimeType", "image/png"
                        )
                );

                String jsonBody = objectMapper.writeValueAsString(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(20))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    Map<String, Object> root = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
                    Object predictionsObj = root.get("predictions");
                    if (predictionsObj instanceof List<?> list && !list.isEmpty()) {
                        Object first = list.get(0);
                        if (first instanceof Map<?, ?> predMap) {
                            String b64 = (String) predMap.get("bytesBase64Encoded");
                            if (b64 != null && !b64.isBlank()) {
                                return Base64.getDecoder().decode(b64.replaceAll("\\s+", ""));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Imagen model {} request failed: {}", model, e.getMessage());
            }
        }
        return null;
    }

    private byte[] tryPollinationsAi(String prompt) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8).replace("+", "%20");
        int seed = 1000 + RANDOM.nextInt(90000);
        String uriStr = String.format("https://image.pollinations.ai/prompt/%s?width=800&height=600&nologo=true&seed=%d", encoded, seed);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uriStr))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) NQD-LMS/1.0")
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 200 && response.body() != null && response.body().length > 1000) {
            return response.body();
        } else {
            log.warn("Pollinations AI returned HTTP status: {}", response.statusCode());
        }
        return null;
    }

    private byte[] generateFallbackSvg(String prompt, String topic) {
        String cleanTitle = (topic != null && !topic.isBlank()) ? topic.trim() : "Hình vẽ minh họa đề bài";
        String cleanDesc = prompt.length() > 100 ? prompt.substring(0, 97) + "..." : prompt;

        String svg = String.format("""
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 500" width="100%%" height="100%%">
                  <defs>
                    <linearGradient id="bg" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                      <stop offset="0%%" stop-color="#f8fafc"/>
                      <stop offset="100%%" stop-color="#f1f5f9"/>
                    </linearGradient>
                    <filter id="shadow" x="-5%%" y="-5%%" width="110%%" height="110%%">
                      <feDropShadow dx="0" dy="4" stdDeviation="6" flood-opacity="0.08"/>
                    </filter>
                  </defs>
                  <rect width="800" height="500" fill="url(#bg)"/>
                  <rect x="40" y="30" width="720" height="440" rx="16" fill="#ffffff" stroke="#cbd5e1" stroke-width="2" filter="url(#shadow)"/>
                  
                  <!-- Educational Diagram Placeholder Illustration -->
                  <g transform="translate(100, 70)">
                    <!-- Grid background -->
                    <pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse">
                      <path d="M 20 0 L 0 0 0 20" fill="none" stroke="#f1f5f9" stroke-width="1"/>
                    </pattern>
                    <rect width="600" height="250" fill="url(#grid)" stroke="#e2e8f0" stroke-width="1.5" rx="8"/>
                    
                    <!-- Geometric / Scientific Elements -->
                    <polygon points="120,210 480,210 120,60" fill="rgba(59, 130, 246, 0.08)" stroke="#2563eb" stroke-width="3"/>
                    <rect x="120" y="190" width="20" height="20" fill="none" stroke="#2563eb" stroke-width="2"/>
                    <circle cx="120" cy="210" r="5" fill="#1d4ed8"/>
                    <text x="95" y="225" font-family="system-ui, sans-serif" font-size="18" font-weight="bold" fill="#1e293b">A</text>
                    
                    <circle cx="480" cy="210" r="5" fill="#1d4ed8"/>
                    <text x="495" y="225" font-family="system-ui, sans-serif" font-size="18" font-weight="bold" fill="#1e293b">B</text>
                    
                    <circle cx="120" cy="60" r="5" fill="#1d4ed8"/>
                    <text x="100" y="55" font-family="system-ui, sans-serif" font-size="18" font-weight="bold" fill="#1e293b">C</text>
                  </g>
                  
                  <text x="400" y="380" font-family="system-ui, sans-serif" font-size="20" font-weight="bold" text-anchor="middle" fill="#0f172a">%s</text>
                  <text x="400" y="415" font-family="system-ui, sans-serif" font-size="14" text-anchor="middle" fill="#64748b">%s</text>
                </svg>
                """, escapeXml(cleanTitle), escapeXml(cleanDesc));

        return svg.getBytes(StandardCharsets.UTF_8);
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String saveImageFile(byte[] imageData, String extension) throws IOException {
        Path uploadDir = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String storedFilename = "ai-img-" + UUID.randomUUID() + "." + extension;
        Path targetPath = uploadDir.resolve(storedFilename);
        Files.write(targetPath, imageData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return "/api/v1/public/media/" + storedFilename;
    }
}
