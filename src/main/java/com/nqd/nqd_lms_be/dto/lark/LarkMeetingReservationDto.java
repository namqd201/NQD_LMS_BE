package com.nqd.nqd_lms_be.dto.lark;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LarkMeetingReservationDto {
    private String meetingUrl;
    private String meetingId;
    private String reserveId;
    private String passcode;
    private String topic;
    private Long endTime;
}
