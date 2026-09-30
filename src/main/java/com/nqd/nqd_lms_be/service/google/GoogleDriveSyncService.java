package com.nqd.nqd_lms_be.service.google;

import java.util.UUID;

public interface GoogleDriveSyncService {
    /**
     * Scan Google Drive for Meet recordings matching the specified classroom and upload them to YouTube.
     * @param classroomId Classroom ID to sync for
     * @return Number of newly uploaded recordings
     */
    int syncRecordingsForClassroom(UUID classroomId);

    /**
     * Scan Google Drive for any Meet recordings and auto-match with active classrooms.
     * @return Number of newly uploaded recordings
     */
    int syncAllMeetRecordings();
}
