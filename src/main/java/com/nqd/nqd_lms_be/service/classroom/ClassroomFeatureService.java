package com.nqd.nqd_lms_be.service.classroom;

import com.nqd.nqd_lms_be.dto.classroom.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ClassroomFeatureService {

    // Materials (Lessons)
    List<ClassroomMaterialDto.Response> getMaterials(UUID classroomId, UUID currentUserId);
    ClassroomMaterialDto.Response getMaterialById(UUID classroomId, UUID materialId, UUID currentUserId);
    ClassroomMaterialDto.Response createMaterial(UUID classroomId, ClassroomMaterialDto.Request request, UUID teacherId);
    ClassroomMaterialDto.Response updateMaterial(UUID classroomId, UUID materialId, ClassroomMaterialDto.Request request, UUID teacherId);
    void deleteMaterial(UUID classroomId, UUID materialId, UUID teacherId);

    // Assignments
    List<ClassroomAssignmentDto.Response> getAssignments(UUID classroomId, UUID currentUserId);
    ClassroomAssignmentDto.Response createAssignment(UUID classroomId, ClassroomAssignmentDto.Request request, UUID teacherId);
    void deleteAssignment(UUID classroomId, UUID assignmentId, UUID teacherId);

    // Live Meeting (Lark)
    ClassroomMeetingDto.Response getMeetingInfo(UUID classroomId, UUID currentUserId);
    ClassroomMeetingDto.Response updateMeetingInfo(UUID classroomId, ClassroomMeetingDto.Request request, UUID teacherId);

    // Recorded Videos
    List<ClassroomRecordedVideoDto.Response> getRecordedVideos(UUID classroomId, UUID currentUserId);
    ClassroomRecordedVideoDto.Response createRecordedVideo(UUID classroomId, ClassroomRecordedVideoDto.Request request, UUID teacherId);
    void deleteRecordedVideo(UUID classroomId, UUID videoId, UUID teacherId);

    // Schedules
    List<ClassroomScheduleDto.Response> getSchedules(UUID classroomId, UUID currentUserId);
    ClassroomScheduleDto.Response createSchedule(UUID classroomId, ClassroomScheduleDto.Request request, UUID teacherId);
    void deleteSchedule(UUID classroomId, UUID scheduleId, UUID teacherId);

    // Files
    List<ClassroomFileDto.Response> getFiles(UUID classroomId, UUID currentUserId);
    ClassroomFileDto.Response createFileRecord(UUID classroomId, ClassroomFileDto.Request request, UUID teacherId);
    ClassroomFileDto.Response uploadClassroomFile(UUID classroomId, MultipartFile file, UUID teacherId);
    void deleteFile(UUID classroomId, UUID fileId, UUID teacherId);
}
