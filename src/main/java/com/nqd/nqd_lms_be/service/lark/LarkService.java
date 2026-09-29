package com.nqd.nqd_lms_be.service.lark;

import com.nqd.nqd_lms_be.dto.lark.LarkMeetingReservationDto;
import com.nqd.nqd_lms_be.dto.lark.LarkRecordingDto;

public interface LarkService {
    /**
     * Get valid Lark Tenant Access Token (cached automatically).
     */
    String getTenantAccessToken();

    /**
     * Reserve / Create a Lark Video Meeting for a classroom.
     *
     * @param topic Topic / Name of the classroom
     * @param durationDays Validity in days (e.g. 30 days)
     * @return Lark meeting reservation details including meeting URL and ID
     */
    LarkMeetingReservationDto createMeetingReservation(String topic, int durationDays);

    /**
     * Fetch recording file download URL for a completed meeting.
     *
     * @param meetingId Lark meeting ID
     * @return Recording info with download URL
     */
    LarkRecordingDto getMeetingRecording(String meetingId);

    /**
     * Permanently delete meeting recording on Lark to free up storage space.
     *
     * @param meetingId Lark meeting ID
     */
    void deleteMeetingRecording(String meetingId);
}
