package com.nqd.nqd_lms_be.ai.audio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AiAudioServiceImpl implements AiAudioService {

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private static final String UPLOAD_DIR = "uploads/media";

    public AiAudioServiceImpl(
            @Value("${lms.ai.gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public String synthesizeSpeech(String script, String preferredVoice) {
        if (script == null || script.isBlank()) {
            return null;
        }

        if (apiKey.isBlank()) {
            log.warn("Gemini API key is not configured for AI Audio Synthesis. Frontend Web Speech fallback will be used.");
            return null;
        }

        String voice = (preferredVoice != null && !preferredVoice.isBlank()) ? preferredVoice.trim() : "Puck";

        // Candidate models supporting audio output
        List<String> ttsModels = List.of(
                "gemini-2.0-flash",
                "gemini-2.5-flash",
                "gemini-3.1-flash-tts-preview"
        );

        for (String model : ttsModels) {
            try {
                String audioUrl = callGeminiTts(model, script, voice);
                if (audioUrl != null) {
                    log.info("Successfully synthesized listening audio via model '{}': {}", model, audioUrl);
                    return audioUrl;
                }
            } catch (Exception e) {
                log.warn("Failed to synthesize audio with model '{}': {}. Trying next...", model, e.getMessage());
            }
        }

        log.warn("All Gemini Audio candidate models failed for script length {}. Returning null (client-side fallback)", script.length());
        return null;
    }

    @Override
    public CompletableFuture<String> synthesizeSpeechAsync(String script, String preferredVoice) {
        return CompletableFuture.supplyAsync(() -> synthesizeSpeech(script, preferredVoice));
    }

    private String callGeminiTts(String model, String script, String voiceName) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        // Clean script prompt for optimal speech flow
        String promptText = "Please read aloud the following English listening passage with clear, natural pronunciation and appropriate pauses:\n\n"
                + script.trim();

        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("contents", List.of(
                Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", promptText))
                )
        ));

        Map<String, Object> voiceConfig = Map.of(
                "prebuilt_voice_config", Map.of("voice_name", voiceName)
        );
        Map<String, Object> speechConfig = Map.of(
                "voice_config", voiceConfig
        );
        Map<String, Object> genConfig = Map.of(
                "response_modalities", List.of("AUDIO"),
                "speech_config", speechConfig
        );
        requestPayload.put("generation_config", genConfig);

        String jsonBody = objectMapper.writeValueAsString(requestPayload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(45))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("Gemini TTS HTTP {} from {}: {}", response.statusCode(), model, response.body());
            return null;
        }

        byte[] pcmData = extractAudioData(response.body());
        if (pcmData == null || pcmData.length == 0) {
            log.warn("No audio data returned in Gemini response: {}", response.body());
            return null;
        }

        // Convert PCM (24000Hz, 1 channel, 16-bit) to standard RIFF WAV format
        byte[] wavBytes = pcmToWav(pcmData, 24000, 1, 16);

        // Ensure upload directory exists
        Path uploadDir = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String storedFilename = "listening-" + UUID.randomUUID() + ".wav";
        Path targetPath = uploadDir.resolve(storedFilename);
        Files.write(targetPath, wavBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return "/api/v1/public/media/" + storedFilename;
    }

    private byte[] extractAudioData(String responseJson) {
        try {
            Map<String, Object> root = objectMapper.readValue(responseJson, new TypeReference<Map<String, Object>>() {});
            Object candidatesObj = root.get("candidates");
            if (candidatesObj instanceof List<?> candidates && !candidates.isEmpty()) {
                Object firstCand = candidates.get(0);
                if (firstCand instanceof Map<?, ?> candMap) {
                    Object contentObj = candMap.get("content");
                    if (contentObj instanceof Map<?, ?> contentMap) {
                        Object partsObj = contentMap.get("parts");
                        if (partsObj instanceof List<?> partsList) {
                            for (Object p : partsList) {
                                if (p instanceof Map<?, ?> pMap) {
                                    Object inlineDataObj = pMap.get("inline_data");
                                    if (inlineDataObj == null) {
                                        inlineDataObj = pMap.get("inlineData");
                                    }
                                    if (inlineDataObj instanceof Map<?, ?> inlineMap) {
                                        Object dataStr = inlineMap.get("data");
                                        if (dataStr instanceof String base64) {
                                            return Base64.getDecoder().decode(base64.replaceAll("\\s+", ""));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract audio from Gemini response JSON: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Converts raw linear PCM audio bytes into a standard playable RIFF WAV file with standard 44-byte header.
     */
    public static byte[] pcmToWav(byte[] pcmData, int sampleRate, int channels, int bitDepth) {
        int totalDataLen = pcmData.length + 36;
        int byteRate = sampleRate * channels * (bitDepth / 8);
        int blockAlign = channels * (bitDepth / 8);

        byte[] header = new byte[44];
        // "RIFF"
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        // "WAVE"
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        // "fmt "
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        // Subchunk1Size = 16 (for PCM)
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        // AudioFormat = 1 (Linear PCM)
        header[20] = 1; header[21] = 0;
        // NumChannels
        header[22] = (byte) channels; header[23] = 0;
        // SampleRate
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        // ByteRate
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // BlockAlign
        header[32] = (byte) blockAlign; header[33] = 0;
        // BitsPerSample
        header[34] = (byte) bitDepth; header[35] = 0;
        // "data"
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (pcmData.length & 0xff);
        header[41] = (byte) ((pcmData.length >> 8) & 0xff);
        header[42] = (byte) ((pcmData.length >> 16) & 0xff);
        header[43] = (byte) ((pcmData.length >> 24) & 0xff);

        byte[] wavBytes = new byte[header.length + pcmData.length];
        System.arraycopy(header, 0, wavBytes, 0, header.length);
        System.arraycopy(pcmData, 0, wavBytes, header.length, pcmData.length);
        return wavBytes;
    }
}
