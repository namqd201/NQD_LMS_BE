package com.nqd.nqd_lms_be.ai.audio;

import java.util.concurrent.CompletableFuture;

public interface AiAudioService {

    /**
     * Synthesize speech for an English listening transcript/dialogue.
     * Generates a standard WAV/MP3 audio file and saves it in the public media folder.
     *
     * @param script Text transcript / dialogue to speak
     * @param preferredVoice e.g. "Puck", "Kore", "Aoede", "Fenrir" (optional)
     * @return Public URL to the synthesized audio (e.g., "/api/v1/public/media/xyz.wav"), or null if unavailable.
     */
    String synthesizeSpeech(String script, String preferredVoice);

    /**
     * Async version of speech synthesis.
     */
    CompletableFuture<String> synthesizeSpeechAsync(String script, String preferredVoice);
}
