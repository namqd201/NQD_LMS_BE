package com.nqd.nqd_lms_be.service.onehundredms;

import java.util.UUID;

public interface OneHundredMsSyncService {
    int syncRecordingsForClassroom(UUID classroomId);
    int syncAll100msRecordings();
}
