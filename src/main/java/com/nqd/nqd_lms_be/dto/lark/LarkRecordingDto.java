package com.nqd.nqd_lms_be.dto.lark;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LarkRecordingDto {
    private String meetingId;
    private String url;
    private Long durationMs;
}
