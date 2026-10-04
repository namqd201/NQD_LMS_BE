package com.nqd.nqd_lms_be.service.lab;

import com.nqd.nqd_lms_be.dto.lab.CreateLabRoomRequest;
import com.nqd.nqd_lms_be.dto.lab.CreateLabVideoRequest;
import com.nqd.nqd_lms_be.dto.lab.LabRecordedVideoResponse;
import com.nqd.nqd_lms_be.dto.lab.LabRoomResponse;
import com.nqd.nqd_lms_be.entity.enums.LabStatus;

import java.util.List;
import java.util.UUID;

public interface LabService {

    LabRoomResponse createLabRoom(UUID hostUserId, CreateLabRoomRequest request);

    List<LabRoomResponse> getPublicLabRooms(UUID currentUserId, boolean isAdmin);

    LabRoomResponse getLabRoomById(UUID labId, UUID currentUserId, boolean isAdmin);

    LabRoomResponse updateLabStatus(UUID labId, UUID currentUserId, boolean isAdmin, LabStatus status);

    void deleteLabRoom(UUID labId, UUID currentUserId, boolean isAdmin);

    List<LabRecordedVideoResponse> getRecordedVideos(UUID currentUserId, boolean isAdmin);

    LabRecordedVideoResponse saveRecordedVideo(UUID labId, UUID currentUserId, boolean isAdmin, CreateLabVideoRequest request);

    int sync100msRecordings(UUID labId, UUID currentUserId, boolean isAdmin);

    void deleteRecordedVideo(UUID videoId, UUID currentUserId, boolean isAdmin);

    com.nqd.nqd_lms_be.dto.lab.LabRoomLivePresenceDto getLivePresence(UUID labId);
}

