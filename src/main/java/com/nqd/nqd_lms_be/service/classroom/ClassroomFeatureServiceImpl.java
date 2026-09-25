package com.nqd.nqd_lms_be.service.classroom;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.classroom.*;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.ClassEnrollmentStatus;
import com.nqd.nqd_lms_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassroomFeatureServiceImpl implements ClassroomFeatureService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final ClassroomMaterialRepository materialRepository;
    private final ClassroomAssignmentRepository assignmentRepository;
    private final ClassroomScheduleRepository scheduleRepository;
    private final ClassroomRecordedVideoRepository videoRepository;
    private final ClassroomFileRepository fileRepository;

    private static final String CLASSROOM_FILES_DIR = "uploads/classroom_files";

    private Classroom validateAndGetClassroomAccess(UUID classroomId, UUID currentUserId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom", classroomId));

        if (currentUserId == null) {
            throw new ForbiddenOperationException("Vui lòng đăng nhập để truy cập thông tin lớp học.");
        }

        // If teacher of the class -> OK
        if (classroom.getTeacher().getId().equals(currentUserId)) {
            return classroom;
        }

        // If enrolled student -> OK
        boolean isEnrolled = classroomStudentRepository.findByClassroomIdAndStudentId(classroomId, currentUserId)
                .map(cs -> cs.getStatus() == ClassEnrollmentStatus.ENROLLED)
                .orElse(false);

        if (!isEnrolled) {
            // Check if ADMIN
            List<String> roleNames = userRoleRepository.findRoleNamesByUserId(currentUserId);
            boolean isAdmin = roleNames != null && roleNames.stream()
                    .anyMatch(r -> r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ROLE_ADMIN"));
            if (!isAdmin) {
                throw new ForbiddenOperationException("Bạn không có quyền truy cập dữ liệu của lớp học này.");
            }
        }

        return classroom;
    }

    private Classroom validateTeacherPermission(UUID classroomId, UUID currentUserId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom", classroomId));

        if (currentUserId == null) {
            throw new ForbiddenOperationException("Vui lòng đăng nhập.");
        }

        if (classroom.getTeacher().getId().equals(currentUserId)) {
            return classroom;
        }

        List<String> roleNames = userRoleRepository.findRoleNamesByUserId(currentUserId);
        boolean isAdmin = roleNames != null && roleNames.stream()
                .anyMatch(r -> r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new ForbiddenOperationException("Chỉ giáo viên phụ trách lớp học mới có quyền thực hiện thao tác này.");
        }

        return classroom;
    }

    // ==========================================
    // 1. MATERIALS (LESSONS)
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomMaterialDto.Response> getMaterials(UUID classroomId, UUID currentUserId) {
        validateAndGetClassroomAccess(classroomId, currentUserId);
        return materialRepository.findByClassroomIdOrderByLessonOrderAscCreatedAtAsc(classroomId)
                .stream()
                .map(this::mapToMaterialResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomMaterialDto.Response getMaterialById(UUID classroomId, UUID materialId, UUID currentUserId) {
        validateAndGetClassroomAccess(classroomId, currentUserId);
        ClassroomMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassroomMaterial", materialId));
        return mapToMaterialResponse(material);
    }

    @Override
    @Transactional
    public ClassroomMaterialDto.Response createMaterial(UUID classroomId, ClassroomMaterialDto.Request request, UUID teacherId) {
        Classroom classroom = validateTeacherPermission(classroomId, teacherId);
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        ClassroomMaterial material = ClassroomMaterial.builder()
                .classroom(classroom)
                .title(request.getTitle().trim())
                .chapterTitle(request.getChapterTitle() != null && !request.getChapterTitle().trim().isEmpty() 
                        ? request.getChapterTitle().trim() : "Chủ đề chung")
                .lessonOrder(request.getLessonOrder() != null ? request.getLessonOrder() : 1)
                .description(request.getDescription())
                .content(request.getContent())
                .videoUrl(request.getVideoUrl() != null && !request.getVideoUrl().trim().isEmpty() 
                        ? request.getVideoUrl().trim() : null)
                .attachmentName(request.getAttachmentName() != null && !request.getAttachmentName().trim().isEmpty()
                        ? request.getAttachmentName().trim() : null)
                .materialType(request.getMaterialType() != null && !request.getMaterialType().trim().isEmpty() 
                        ? request.getMaterialType().trim() : "LESSON")
                .fileUrl(request.getFileUrl() != null && !request.getFileUrl().trim().isEmpty() 
                        ? request.getFileUrl().trim() : null)
                .uploadedBy(teacher)
                .downloadCount(0)
                .build();

        material = materialRepository.save(material);
        log.info("Teacher {} created lesson/material: {} for classroom {}", teacher.getEmail(), material.getTitle(), classroom.getName());
        return mapToMaterialResponse(material);
    }

    @Override
    @Transactional
    public ClassroomMaterialDto.Response updateMaterial(UUID classroomId, UUID materialId, ClassroomMaterialDto.Request request, UUID teacherId) {
        validateTeacherPermission(classroomId, teacherId);
        ClassroomMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassroomMaterial", materialId));

        material.setTitle(request.getTitle().trim());
        if (request.getChapterTitle() != null) {
            material.setChapterTitle(request.getChapterTitle().trim().isEmpty() ? "Chủ đề chung" : request.getChapterTitle().trim());
        }
        if (request.getLessonOrder() != null) {
            material.setLessonOrder(request.getLessonOrder());
        }
        material.setDescription(request.getDescription());
        material.setContent(request.getContent());
        material.setVideoUrl(request.getVideoUrl() != null && !request.getVideoUrl().trim().isEmpty() 
                ? request.getVideoUrl().trim() : null);
        material.setAttachmentName(request.getAttachmentName() != null && !request.getAttachmentName().trim().isEmpty()
                ? request.getAttachmentName().trim() : null);
        if (request.getMaterialType() != null && !request.getMaterialType().trim().isEmpty()) {
            material.setMaterialType(request.getMaterialType().trim());
        }
        material.setFileUrl(request.getFileUrl() != null && !request.getFileUrl().trim().isEmpty() 
                ? request.getFileUrl().trim() : null);

        material = materialRepository.save(material);
        log.info("Teacher {} updated lesson/material: {} for classroom {}", teacherId, material.getTitle(), classroomId);
        return mapToMaterialResponse(material);
    }

    @Override
    @Transactional
    public void deleteMaterial(UUID classroomId, UUID materialId, UUID teacherId) {
        validateTeacherPermission(classroomId, teacherId);
        ClassroomMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassroomMaterial", materialId));
        materialRepository.delete(material);
    }

    private ClassroomMaterialDto.Response mapToMaterialResponse(ClassroomMaterial m) {
        return ClassroomMaterialDto.Response.builder()
                .id(m.getId())
                .classroomId(m.getClassroom().getId())
                .title(m.getTitle())
                .chapterTitle(m.getChapterTitle() != null ? m.getChapterTitle() : "Chủ đề chung")
                .lessonOrder(m.getLessonOrder() != null ? m.getLessonOrder() : 1)
                .description(m.getDescription())
                .content(m.getContent())
                .videoUrl(m.getVideoUrl())
                .materialType(m.getMaterialType())
                .fileUrl(m.getFileUrl())
                .attachmentName(m.getAttachmentName())
                .uploadedById(m.getUploadedBy().getId())
                .uploadedByName(m.getUploadedBy().getFullName())
                .downloadCount(m.getDownloadCount())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    // ==========================================
    // 2. ASSIGNMENTS
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomAssignmentDto.Response> getAssignments(UUID classroomId, UUID currentUserId) {
        validateAndGetClassroomAccess(classroomId, currentUserId);
        return assignmentRepository.findByClassroomIdOrderByCreatedAtDesc(classroomId)
                .stream()
                .map(this::mapToAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClassroomAssignmentDto.Response createAssignment(UUID classroomId, ClassroomAssignmentDto.Request request, UUID teacherId) {
        Classroom classroom = validateTeacherPermission(classroomId, teacherId);
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        ClassroomAssignment assignment = ClassroomAssignment.builder()
                .classroom(classroom)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .maxScore(request.getMaxScore() != null ? request.getMaxScore() : 10)
                .attachmentUrl(request.getAttachmentUrl())
                .assignedBy(teacher)
                .status("ACTIVE")
                .build();

        assignment = assignmentRepository.save(assignment);
        log.info("Teacher {} created assignment: {} for classroom {}", teacher.getEmail(), assignment.getTitle(), classroom.getName());
        return mapToAssignmentResponse(assignment);
    }

    @Override
    @Transactional
    public void deleteAssignment(UUID classroomId, UUID assignmentId, UUID teacherId) {
        validateTeacherPermission(classroomId, teacherId);
        ClassroomAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassroomAssignment", assignmentId));
        assignmentRepository.delete(assignment);
    }

    private ClassroomAssignmentDto.Response mapToAssignmentResponse(ClassroomAssignment a) {
        return ClassroomAssignmentDto.Response.builder()
                .id(a.getId())
                .classroomId(a.getClassroom().getId())
                .title(a.getTitle())
                .description(a.getDescription())
                .deadline(a.getDeadline())
                .maxScore(a.getMaxScore())
                .attachmentUrl(a.getAttachmentUrl())
                .status(a.getStatus())
                .assignedById(a.getAssignedBy().getId())
                .assignedByName(a.getAssignedBy().getFullName())
                .createdAt(a.getCreatedAt())
                .build();
    }

    // ==========================================
    // 3. LIVE MEETING (LARK)
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public ClassroomMeetingDto.Response getMeetingInfo(UUID classroomId, UUID currentUserId) {
        Classroom c = validateAndGetClassroomAccess(classroomId, currentUserId);
        return ClassroomMeetingDto.Response.builder()
                .classroomId(c.getId())
                .larkMeetingUrl(c.getLarkMeetingUrl())
                .meetingId(c.getMeetingId())
                .passcode(c.getPasscode())
                .meetingNote(c.getMeetingNote())
                .isLiveNow(Boolean.TRUE.equals(c.getIsLiveNow()))
                .build();
    }

    @Override
    @Transactional
    public ClassroomMeetingDto.Response updateMeetingInfo(UUID classroomId, ClassroomMeetingDto.Request request, UUID teacherId) {
        Classroom c = validateTeacherPermission(classroomId, teacherId);

        c.setLarkMeetingUrl(request.getLarkMeetingUrl() != null ? request.getLarkMeetingUrl().trim() : null);
        c.setMeetingId(request.getMeetingId() != null ? request.getMeetingId().trim() : null);
        c.setPasscode(request.getPasscode() != null ? request.getPasscode().trim() : null);
        c.setMeetingNote(request.getMeetingNote());
        if (request.getIsLiveNow() != null) {
            c.setIsLiveNow(request.getIsLiveNow());
        }

        c = classroomRepository.save(c);
        log.info("Teacher {} updated live meeting settings for classroom {}", teacherId, c.getName());

        return ClassroomMeetingDto.Response.builder()
                .classroomId(c.getId())
                .larkMeetingUrl(c.getLarkMeetingUrl())
                .meetingId(c.getMeetingId())
                .passcode(c.getPasscode())
                .meetingNote(c.getMeetingNote())
                .isLiveNow(Boolean.TRUE.equals(c.getIsLiveNow()))
                .build();
    }

    // ==========================================
    // 4. RECORDED VIDEOS (LARK VIDEOS)
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomRecordedVideoDto.Response> getRecordedVideos(UUID classroomId, UUID currentUserId) {
        validateAndGetClassroomAccess(classroomId, currentUserId);
        return videoRepository.findByClassroomIdOrderBySessionDateDescCreatedAtDesc(classroomId)
                .stream()
                .map(this::mapToVideoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClassroomRecordedVideoDto.Response createRecordedVideo(UUID classroomId, ClassroomRecordedVideoDto.Request request, UUID teacherId) {
        Classroom classroom = validateTeacherPermission(classroomId, teacherId);
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        ClassroomRecordedVideo video = ClassroomRecordedVideo.builder()
                .classroom(classroom)
                .title(request.getTitle().trim())
                .videoUrl(request.getVideoUrl().trim())
                .sessionDate(request.getSessionDate())
                .durationMinutes(request.getDurationMinutes())
                .description(request.getDescription())
                .uploadedBy(teacher)
                .build();

        video = videoRepository.save(video);
        return mapToVideoResponse(video);
    }

    @Override
    @Transactional
    public void deleteRecordedVideo(UUID classroomId, UUID videoId, UUID teacherId) {
        validateTeacherPermission(classroomId, teacherId);
        ClassroomRecordedVideo video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassroomRecordedVideo", videoId));
        videoRepository.delete(video);
    }

    private ClassroomRecordedVideoDto.Response mapToVideoResponse(ClassroomRecordedVideo v) {
        return ClassroomRecordedVideoDto.Response.builder()
                .id(v.getId())
                .classroomId(v.getClassroom().getId())
                .title(v.getTitle())
                .videoUrl(v.getVideoUrl())
                .sessionDate(v.getSessionDate())
                .durationMinutes(v.getDurationMinutes())
                .description(v.getDescription())
                .uploadedById(v.getUploadedBy().getId())
                .uploadedByName(v.getUploadedBy().getFullName())
                .createdAt(v.getCreatedAt())
                .build();
    }

    // ==========================================
    // 5. SCHEDULES
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomScheduleDto.Response> getSchedules(UUID classroomId, UUID currentUserId) {
        validateAndGetClassroomAccess(classroomId, currentUserId);
        return scheduleRepository.findByClassroomIdOrderByDayOfWeekAscStartTimeAsc(classroomId)
                .stream()
                .map(this::mapToScheduleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClassroomScheduleDto.Response createSchedule(UUID classroomId, ClassroomScheduleDto.Request request, UUID teacherId) {
        Classroom classroom = validateTeacherPermission(classroomId, teacherId);

        ClassroomSchedule schedule = ClassroomSchedule.builder()
                .classroom(classroom)
                .dayOfWeek(request.getDayOfWeek().trim().toUpperCase())
                .startTime(request.getStartTime().trim())
                .endTime(request.getEndTime().trim())
                .title(request.getTitle().trim())
                .roomNote(request.getRoomNote())
                .build();

        schedule = scheduleRepository.save(schedule);
        return mapToScheduleResponse(schedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(UUID classroomId, UUID scheduleId, UUID teacherId) {
        validateTeacherPermission(classroomId, teacherId);
        ClassroomSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassroomSchedule", scheduleId));
        scheduleRepository.delete(schedule);
    }

    private ClassroomScheduleDto.Response mapToScheduleResponse(ClassroomSchedule s) {
        return ClassroomScheduleDto.Response.builder()
                .id(s.getId())
                .classroomId(s.getClassroom().getId())
                .dayOfWeek(s.getDayOfWeek())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .title(s.getTitle())
                .roomNote(s.getRoomNote())
                .build();
    }

    // ==========================================
    // 6. FILES
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomFileDto.Response> getFiles(UUID classroomId, UUID currentUserId) {
        validateAndGetClassroomAccess(classroomId, currentUserId);
        return fileRepository.findByClassroomIdOrderByCreatedAtDesc(classroomId)
                .stream()
                .map(this::mapToFileResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClassroomFileDto.Response createFileRecord(UUID classroomId, ClassroomFileDto.Request request, UUID teacherId) {
        Classroom classroom = validateTeacherPermission(classroomId, teacherId);
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        ClassroomFile f = ClassroomFile.builder()
                .classroom(classroom)
                .fileName(request.getFileName().trim())
                .fileUrl(request.getFileUrl().trim())
                .fileSize(request.getFileSize())
                .fileType(request.getFileType())
                .uploadedBy(teacher)
                .build();

        f = fileRepository.save(f);
        return mapToFileResponse(f);
    }

    @Override
    @Transactional
    public ClassroomFileDto.Response uploadClassroomFile(UUID classroomId, MultipartFile file, UUID teacherId) {
        Classroom classroom = validateTeacherPermission(classroomId, teacherId);
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tập tin tải lên không được để trống.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }

        try {
            Path uploadPath = Paths.get(CLASSROOM_FILES_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String storedFilename = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);
            Path targetLocation = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/api/v1/public/classroom-files/" + storedFilename;

            ClassroomFile cf = ClassroomFile.builder()
                .classroom(classroom)
                .fileName(originalFilename != null ? originalFilename : storedFilename)
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .fileType(extension)
                .uploadedBy(teacher)
                .build();

            cf = fileRepository.save(cf);
            log.info("Teacher {} uploaded classroom file: {} ({}) for classroom {}", teacher.getEmail(), originalFilename, fileUrl, classroom.getName());
            return mapToFileResponse(cf);
        } catch (IOException e) {
            log.error("Failed to store classroom file: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể lưu tệp tin: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteFile(UUID classroomId, UUID fileId, UUID teacherId) {
        validateTeacherPermission(classroomId, teacherId);
        ClassroomFile f = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassroomFile", fileId));
        fileRepository.delete(f);
    }

    private ClassroomFileDto.Response mapToFileResponse(ClassroomFile f) {
        return ClassroomFileDto.Response.builder()
                .id(f.getId())
                .classroomId(f.getClassroom().getId())
                .fileName(f.getFileName())
                .fileUrl(f.getFileUrl())
                .fileSize(f.getFileSize())
                .fileType(f.getFileType())
                .uploadedById(f.getUploadedBy().getId())
                .uploadedByName(f.getUploadedBy().getFullName())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
