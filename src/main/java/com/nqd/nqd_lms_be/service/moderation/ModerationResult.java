package com.nqd.nqd_lms_be.service.moderation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {
    private boolean isViolated;
    private String reason;
    private String category;

    public static ModerationResult clean() {
        return ModerationResult.builder()
                .isViolated(false)
                .reason(null)
                .category("CLEAN")
                .build();
    }

    public static ModerationResult violated(String reason, String category) {
        return ModerationResult.builder()
                .isViolated(true)
                .reason(reason)
                .category(category)
                .build();
    }
}
