package com.nqd.nqd_lms_be.controller.classroom;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.classroom.*;
import com.nqd.nqd_lms_be.service.classroom.ClassroomFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/classrooms/{classroomId}")
@RequiredArgsConstructor
@Tag(name = "Classroom Features", description = "Endpoints for materials, assignments, online meeting, schedule, and files")
public class ClassroomFeatureController {

    private final ClassroomFeatureService featureService;
    private static final String CLASSROOM_FILES_DIR = "uploads/classroom_files";

    // ==========================================
    // 1. MATERIALS (TÀI LIỆU)
    // ==========================================

    @GetMapping("/materials")
    @Operation(summary = "Lấy danh sách tài liệu học tập trong lớp")
    public ResponseEntity<List<ClassroomMaterialDto.Response>> getMaterials(
            @PathVariable UUID classroomId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(featureService.getMaterials(classroomId, principal.getId()));
    }

    @PostMapping("/materials")
    @Operation(summary = "Giáo viên đăng tài liệu học tập mới")
    public ResponseEntity<ClassroomMaterialDto.Response> createMaterial(
            @PathVariable UUID classroomId,
            @Valid @RequestBody ClassroomMaterialDto.Request request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(featureService.createMaterial(classroomId, request, principal.getId()));
    }

    @DeleteMapping("/materials/{materialId}")
    @Operation(summary = "Giáo viên xóa tài liệu học tập")
    public ResponseEntity<MessageResponse> deleteMaterial(
            @PathVariable UUID classroomId,
            @PathVariable UUID materialId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        featureService.deleteMaterial(classroomId, materialId, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã xóa tài liệu học tập thành công."));
    }

    // ==========================================
    // 2. ASSIGNMENTS (BÀI TẬP)
    // ==========================================

    @GetMapping("/assignments")
    @Operation(summary = "Lấy danh sách bài tập của lớp học")
    public ResponseEntity<List<ClassroomAssignmentDto.Response>> getAssignments(
            @PathVariable UUID classroomId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(featureService.getAssignments(classroomId, principal.getId()));
    }

    @PostMapping("/assignments")
    @Operation(summary = "Giáo viên giao bài tập mới cho lớp")
    public ResponseEntity<ClassroomAssignmentDto.Response> createAssignment(
            @PathVariable UUID classroomId,
            @Valid @RequestBody ClassroomAssignmentDto.Request request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(featureService.createAssignment(classroomId, request, principal.getId()));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @Operation(summary = "Giáo viên xóa bài tập")
    public ResponseEntity<MessageResponse> deleteAssignment(
            @PathVariable UUID classroomId,
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        featureService.deleteAssignment(classroomId, assignmentId, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã xóa bài tập thành công."));
    }

    // ==========================================
    // 3. LIVE ONLINE MEETING (LARK MEETING)
    // ==========================================

    @GetMapping("/meeting")
    @Operation(summary = "Lấy thông tin phòng học trực tuyến (Lark)")
    public ResponseEntity<ClassroomMeetingDto.Response> getMeetingInfo(
            @PathVariable UUID classroomId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(featureService.getMeetingInfo(classroomId, principal.getId()));
    }

    @PutMapping("/meeting")
    @Operation(summary = "Giáo viên cập nhật link & phòng học trực tuyến (Lark)")
    public ResponseEntity<ClassroomMeetingDto.Response> updateMeetingInfo(
            @PathVariable UUID classroomId,
            @Valid @RequestBody ClassroomMeetingDto.Request request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(featureService.updateMeetingInfo(classroomId, request, principal.getId()));
    }

    // ==========================================
    // 4. RECORDED VIDEOS (VIDEO BẢN GHI LARK)
    // ==========================================

    @GetMapping("/videos")
    @Operation(summary = "Lấy danh sách video bản ghi các buổi học online")
    public ResponseEntity<List<ClassroomRecordedVideoDto.Response>> getRecordedVideos(
            @PathVariable UUID classroomId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(featureService.getRecordedVideos(classroomId, principal.getId()));
    }

    @PostMapping("/videos")
    @Operation(summary = "Giáo viên đăng video bản ghi buổi học online mới")
    public ResponseEntity<ClassroomRecordedVideoDto.Response> createRecordedVideo(
            @PathVariable UUID classroomId,
            @Valid @RequestBody ClassroomRecordedVideoDto.Request request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(featureService.createRecordedVideo(classroomId, request, principal.getId()));
    }

    @DeleteMapping("/videos/{videoId}")
    @Operation(summary = "Giáo viên xóa video bản ghi buổi học")
    public ResponseEntity<MessageResponse> deleteRecordedVideo(
            @PathVariable UUID classroomId,
            @PathVariable UUID videoId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        featureService.deleteRecordedVideo(classroomId, videoId, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã xóa video buổi học thành công."));
    }

    // ==========================================
    // 5. SCHEDULES (LỊCH HỌC)
    // ==========================================

    @GetMapping("/schedules")
    @Operation(summary = "Lấy thời khóa biểu học của lớp")
    public ResponseEntity<List<ClassroomScheduleDto.Response>> getSchedules(
            @PathVariable UUID classroomId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(featureService.getSchedules(classroomId, principal.getId()));
    }

    @PostMapping("/schedules")
    @Operation(summary = "Giáo viên thêm thời khóa biểu buổi học")
    public ResponseEntity<ClassroomScheduleDto.Response> createSchedule(
            @PathVariable UUID classroomId,
            @Valid @RequestBody ClassroomScheduleDto.Request request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(featureService.createSchedule(classroomId, request, principal.getId()));
    }

    @DeleteMapping("/schedules/{scheduleId}")
    @Operation(summary = "Giáo viên xóa lịch học")
    public ResponseEntity<MessageResponse> deleteSchedule(
            @PathVariable UUID classroomId,
            @PathVariable UUID scheduleId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        featureService.deleteSchedule(classroomId, scheduleId, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã xóa lịch học thành công."));
    }

    // ==========================================
    // 6. FILES (TỆP TIN LƯU TRỮ)
    // ==========================================

    @GetMapping("/files")
    @Operation(summary = "Lấy danh sách tệp tin lưu trữ trong lớp học")
    public ResponseEntity<List<ClassroomFileDto.Response>> getFiles(
            @PathVariable UUID classroomId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(featureService.getFiles(classroomId, principal.getId()));
    }

    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Giáo viên tải tệp tin lên kho lưu trữ lớp học")
    public ResponseEntity<ClassroomFileDto.Response> uploadFile(
            @PathVariable UUID classroomId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(featureService.uploadClassroomFile(classroomId, file, principal.getId()));
    }

    @PostMapping("/files")
    @Operation(summary = "Giáo viên tạo bản ghi tệp tin bằng đường dẫn trực tiếp")
    public ResponseEntity<ClassroomFileDto.Response> createFileRecord(
            @PathVariable UUID classroomId,
            @Valid @RequestBody ClassroomFileDto.Request request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(featureService.createFileRecord(classroomId, request, principal.getId()));
    }

    @DeleteMapping("/files/{fileId}")
    @Operation(summary = "Giáo viên xóa tệp tin khỏi lớp học")
    public ResponseEntity<MessageResponse> deleteFile(
            @PathVariable UUID classroomId,
            @PathVariable UUID fileId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        featureService.deleteFile(classroomId, fileId, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã xóa tệp tin thành công."));
    }
}
