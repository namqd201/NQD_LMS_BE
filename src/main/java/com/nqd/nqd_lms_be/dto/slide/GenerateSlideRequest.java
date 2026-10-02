package com.nqd.nqd_lms_be.dto.slide;

import com.nqd.nqd_lms_be.entity.enums.SlideTargetType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateSlideRequest {

    @NotNull(message = "Target type is required")
    private SlideTargetType targetType;

    @NotNull(message = "Target ID is required")
    private UUID targetId;

    @Builder.Default
    private Integer slideCount = 8;

    @Builder.Default
    private String style = "STANDARD"; // STANDARD, CONCISE, DETAILED, EXAM_PREP

    @Builder.Default
    private String language = "vi";
}
