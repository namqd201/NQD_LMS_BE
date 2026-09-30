package com.nqd.nqd_lms_be.service.google;

import com.nqd.nqd_lms_be.dto.google.GoogleMeetReservationDto;

public interface GoogleMeetService {
    GoogleMeetReservationDto createClassroomMeeting(String classroomName, String description);
    void deleteMeetingEvent(String eventId);
}
