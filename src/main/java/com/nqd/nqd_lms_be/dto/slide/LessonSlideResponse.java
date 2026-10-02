package com.nqd.nqd_lms_be.dto.slide;

import com.nqd.nqd_lms_be.entity.enums.SlideTargetType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonSlideResponse {
    private UUID id;
    private SlideTargetType targetType;
    private UUID targetId;
    private String title;
    private String slideUrl;
    private String fileName;
    private String slideContentJson;
    @Builder.Default
    private List<SlideItemDto> slides = new ArrayList<>();
    private String slideSource;
    private Integer slideCount;
    private UUID creatorId;
    private Boolean canManage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
