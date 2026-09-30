package com.nqd.nqd_lms_be.dto.onehundredms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneHundredMsRecordingAssetDto {
    private String id;
    private String roomId;
    private String sessionId;
    private String status;
    private String type;
    private Long duration;
    private String createdAt;
    private String presignedUrl;
}
