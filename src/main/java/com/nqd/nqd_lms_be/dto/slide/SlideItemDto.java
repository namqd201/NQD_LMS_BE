package com.nqd.nqd_lms_be.dto.slide;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlideItemDto {
    private Integer slideNumber;
    private String title;
    private String subtitle;
    /**
     * TITLE, INTRO, CONTENT, SPLIT, FORMULA, SUMMARY
     */
    @Builder.Default
    private String layout = "CONTENT";
    @Builder.Default
    private List<String> bulletPoints = new ArrayList<>();
    private String formula;
    private String callout;
    private String speakerNotes;
}
