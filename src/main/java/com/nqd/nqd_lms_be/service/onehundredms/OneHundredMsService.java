package com.nqd.nqd_lms_be.service.onehundredms;

import com.nqd.nqd_lms_be.dto.onehundredms.OneHundredMsRecordingAssetDto;
import com.nqd.nqd_lms_be.dto.onehundredms.OneHundredMsRoomDto;

import java.util.List;

public interface OneHundredMsService {

    boolean isConfigured();

    OneHundredMsRoomDto createClassroomMeeting(String classroomName, String description);

    void disableMeeting(String roomId);

    List<OneHundredMsRecordingAssetDto> getCompletedRecordingAssets(String roomId);

    String getPresignedDownloadUrl(String assetId);
}
