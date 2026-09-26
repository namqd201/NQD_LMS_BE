package com.nqd.nqd_lms_be.ai.image;

import java.util.concurrent.CompletableFuture;

public interface AiImageService {

    /**
     * Generates a contextually relevant educational diagram/illustration based on prompt and subject/topic.
     * The image is saved locally into `uploads/media` and served via `/api/v1/public/media/ai-img-{UUID}.png`.
     *
     * @param imagePrompt Specific prompt describing the diagram/illustration needed for the question
     * @param topic Topic or subject context (e.g., "Hình học", "Vật lý quang học")
     * @return Public URL to the generated image (e.g., "/api/v1/public/media/ai-img-xyz.png"), or null if failed.
     */
    String generateImage(String imagePrompt, String topic);

    /**
     * Async version of image generation.
     */
    CompletableFuture<String> generateImageAsync(String imagePrompt, String topic);
}
