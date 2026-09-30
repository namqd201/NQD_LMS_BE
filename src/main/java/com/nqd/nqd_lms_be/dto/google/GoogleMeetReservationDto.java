package com.nqd.nqd_lms_be.dto.google;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleMeetReservationDto {
    private String meetingUrl;
    private String eventId;
    private String meetingCode;
    private String summary;
}
