package com.nqd.nqd_lms_be.ai.audio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AiAudioServiceImpl implements AiAudioService {

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private static final String UPLOAD_DIR = "uploads/media";

    // 24000Hz, 16-bit, 1 channel PCM: 1 sec = 48000 bytes. 600ms = 28800 bytes
    private static final int SAMPLE_RATE = 24000;
    private static final int CHANNELS = 1;
    private static final int BIT_DEPTH = 16;
    private static final int PAUSE_BYTES = 28800; // 600ms pause between turns

    private static final List<String> TTS_MODELS = List.of(
            "gemini-2.5-flash-preview-tts",
            "gemini-3.1-flash-tts-preview",
            "gemini-3.8-flash-tts",
            "gemini-2.5-flash",
            "gemini-2.0-flash"
    );

    @Getter
    public static class DialogueTurn {
        private final String speaker;
        private final String text;
        private final boolean female;

        public DialogueTurn(String speaker, String text, boolean female) {
            this.speaker = speaker;
            this.text = text;
            this.female = female;
        }
    }

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

        List<DialogueTurn> turns = parseScript(script);

        // Check if multi-speaker dialogue
        boolean isMultiSpeaker = turns.size() >= 2 && hasMultipleSpeakers(turns);

        if (isMultiSpeaker) {
            log.info("Detected multi-speaker dialogue with {} turns. Synthesizing character-by-character...", turns.size());
            String dialogueAudio = synthesizeDialogue(turns);
            if (dialogueAudio != null) {
                return dialogueAudio;
            }
            log.warn("Multi-speaker dialogue synthesis failed, falling back to full script reading...");
        }

        // Monologue / Single passage synthesis
        String voice = (preferredVoice != null && !preferredVoice.isBlank()) ? preferredVoice.trim() : "Puck";
        // Clean any leading speaker label from monologue so it isn't read aloud
        String cleanScript = cleanMonologueScript(script);

        for (String model : TTS_MODELS) {
            try {
                byte[] pcmData = callGeminiTtsSingleLine(model, cleanScript, voice);
                if (pcmData != null && pcmData.length > 0) {
                    return saveWavFile(pcmData);
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

    /**
     * Synthesizes each dialogue line using that character's designated voice (Male: Puck, Female: Kore),
     * omitting character names from spoken audio, and concatenates lines with a natural 600ms silence.
     */
    private String synthesizeDialogue(List<DialogueTurn> turns) {
        try {
            ByteArrayOutputStream combinedPcm = new ByteArrayOutputStream();
            byte[] pauseBytes = new byte[PAUSE_BYTES]; // 600ms silence

            for (int i = 0; i < turns.size(); i++) {
                DialogueTurn turn = turns.get(i);
                String lineText = turn.getText().trim();
                if (lineText.isEmpty()) continue;

                // Pick distinct voice: Puck for Male, Kore for Female
                String voiceName = turn.isFemale() ? "Kore" : "Puck";

                byte[] turnPcm = null;
                for (String model : TTS_MODELS) {
                    try {
                        turnPcm = callGeminiTtsSingleLine(model, lineText, voiceName);
                        if (turnPcm != null && turnPcm.length > 0) {
                            break;
                        }
                    } catch (Exception e) {
                        log.debug("Turn {} TTS failed with model {}: {}", i, model, e.getMessage());
                    }
                }

                if (turnPcm == null || turnPcm.length == 0) {
                    log.warn("Failed to synthesize dialogue turn {}: '{}' by {}", i, lineText, turn.getSpeaker());
                    return null; // Fallback to whole script
                }

                // Append pause between lines
                if (combinedPcm.size() > 0) {
                    combinedPcm.write(pauseBytes);
                }
                combinedPcm.write(turnPcm);
            }

            byte[] fullPcm = combinedPcm.toByteArray();
            if (fullPcm.length > 0) {
                return saveWavFile(fullPcm);
            }
        } catch (Exception e) {
            log.warn("Error synthesizing dialogue: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Calls Gemini TTS to synthesize a single line or passage without character prefixes.
     */
    private byte[] callGeminiTtsSingleLine(String model, String text, String voiceName) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        String promptText = "Please read aloud clearly and naturally in standard English:\n\n" + text.trim();

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
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("Gemini TTS HTTP {} from {}: {}", response.statusCode(), model, response.body());
            return null;
        }

        return extractAudioData(response.body());
    }

    /**
     * Parses the transcript into structured dialogue turns (speaker, text, gender).
     */
    public List<DialogueTurn> parseScript(String script) {
        if (script == null || script.isBlank()) {
            return Collections.emptyList();
        }
        List<DialogueTurn> turns = new ArrayList<>();
        String[] lines = script.split("\\r?\\n");
        Pattern pattern = Pattern.compile("^(?:[-*•]\\s*)?\\[?([A-Za-z0-9\\s._'-]+?)\\]?\\s*:\\s*(.+)$");

        Map<String, Boolean> speakerGenders = new HashMap<>();
        int unknownSpeakerCount = 0;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            Matcher m = pattern.matcher(line);
            if (m.matches()) {
                String speaker = m.group(1).trim();
                String text = m.group(2).trim();

                boolean isFemale;
                String lowerSpeaker = speaker.toLowerCase();
                if (!speakerGenders.containsKey(lowerSpeaker)) {
                    isFemale = determineSpeakerGender(speaker, unknownSpeakerCount++);
                    speakerGenders.put(lowerSpeaker, isFemale);
                } else {
                    isFemale = speakerGenders.get(lowerSpeaker);
                }

                turns.add(new DialogueTurn(speaker, text, isFemale));
            } else {
                // Continuation line or narration
                if (!turns.isEmpty()) {
                    DialogueTurn last = turns.remove(turns.size() - 1);
                    turns.add(new DialogueTurn(last.speaker, last.text + " " + line, last.female));
                } else {
                    turns.add(new DialogueTurn(null, line, false));
                }
            }
        }
        return turns;
    }

    private boolean hasMultipleSpeakers(List<DialogueTurn> turns) {
        Set<String> distinct = new HashSet<>();
        for (DialogueTurn t : turns) {
            if (t.getSpeaker() != null && !t.getSpeaker().isBlank()) {
                distinct.add(t.getSpeaker().toLowerCase().trim());
            }
        }
        return distinct.size() >= 2;
    }

    /**
     * Determines whether a speaker is female based on name/label, or alternates if unknown.
     */
    public static boolean determineSpeakerGender(String speaker, int index) {
        if (speaker == null || speaker.isBlank()) {
            return (index % 2 == 1);
        }
        String s = speaker.toLowerCase().trim();
        // Female indicators
        if (s.matches(".*\\b(female|woman|girl|lady|mother|mom|sister|daughter|mrs|ms|miss|mai|mary|anna|linda|sarah|emma|jane|hoa|lan|nga|huong|alice|lucy|daisy|jennifer|elizabeth|kate|helen|amy|chloe|zoe|emily|sally|lily|grace)\\b.*")) {
            return true;
        }
        // Male indicators
        if (s.matches(".*\\b(male|man|boy|guy|gentleman|father|dad|brother|son|mr|peter|john|david|tom|bob|nam|minh|quan|huy|alex|mike|james|george|paul|jack|mark|ben|dan|sam|tim|tony|nick|bill|steve)\\b.*")) {
            return false;
        }
        // Alternate if unknown: 0 = Male, 1 = Female, 2 = Male, etc.
        return (index % 2 == 1);
    }

    private String cleanMonologueScript(String script) {
        if (script == null) return "";
        // If whole script has single label like "Announcer: ...", strip it
        return script.replaceAll("^(?:[-*•]\\s*)?\\[?[A-Za-z0-9\\s._'-]+?\\]?\\s*:\\s*", "").trim();
    }

    private String saveWavFile(byte[] pcmData) throws IOException {
        byte[] wavBytes = pcmToWav(pcmData, SAMPLE_RATE, CHANNELS, BIT_DEPTH);

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
