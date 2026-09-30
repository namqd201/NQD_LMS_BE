package com.nqd.nqd_lms_be.dto.onehundredms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneHundredMsRoomDto {
    private String roomId;
    private String roomName;
    private String templateId;
    private String hostCode;
    private String guestCode;
    private String hostMeetingUrl;
    private String guestMeetingUrl;
    private boolean enabled;
}
