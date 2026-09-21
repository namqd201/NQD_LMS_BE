package com.nqd.nqd_lms_be.service.moderation;

public interface ContentModerationService {

    /**
     * Check if the provided text violates community standards or contains toxicity / offensive content.
     *
     * @param text The input text to check
     * @return ModerationResult with violation details
     */
    ModerationResult checkContent(String text);

    /**
     * Validate the provided text. If violated, throws an IllegalArgumentException with a clear message.
     *
     * @param text The input text to validate
     * @param contextName Name of the context (e.g. "Đánh giá", "Bình luận", "Tiêu đề thảo luận")
     */
    void validateContent(String text, String contextName);
}
