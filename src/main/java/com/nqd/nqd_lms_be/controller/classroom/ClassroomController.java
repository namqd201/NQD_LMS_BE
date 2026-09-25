package com.nqd.nqd_lms_be.controller.classroom;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.MessageResponse;
import com.nqd.nqd_lms_be.dto.classroom.*;
import com.nqd.nqd_lms_be.entity.enums.ClassEnrollmentStatus;
import com.nqd.nqd_lms_be.service.classroom.ClassroomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
@Tag(name = "Classroom Management", description = "Endpoints for managing classrooms, enrollments, and invitations")
public class ClassroomController {

    private final ClassroomService classroomService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Tạo lớp học mới (Giáo viên phụ trách)")
    public ResponseEntity<ClassroomResponse> createClassroom(
            @Valid @RequestBody ClassroomRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(classroomService.createClassroom(request, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Cập nhật thông tin lớp học")
    public ResponseEntity<ClassroomResponse> updateClassroom(
            @PathVariable UUID id,
            @Valid @RequestBody ClassroomRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(classroomService.updateClassroom(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Xóa hoặc lưu trữ lớp học")
    public ResponseEntity<MessageResponse> deleteClassroom(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        classroomService.deleteClassroom(id, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã lưu trữ lớp học thành công."));
    }

    @GetMapping("/teaching")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Lấy danh sách các lớp học do giáo viên phụ trách")
    public ResponseEntity<List<ClassroomResponse>> getTeachingClassrooms(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(classroomService.getTeachingClassrooms(principal.getId()));
    }

    @GetMapping("/enrolled")
    @Operation(summary = "Lấy danh sách các lớp học mà học sinh đang tham gia")
    public ResponseEntity<List<ClassroomResponse>> getEnrolledClassrooms(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(classroomService.getEnrolledClassrooms(principal.getId()));
    }

    @GetMapping("/invitations")
    @Operation(summary = "Lấy danh sách lời mời vào lớp học gửi tới người dùng")
    public ResponseEntity<List<ClassroomStudentResponse>> getMyInvitations(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(classroomService.getPendingInvitations(principal.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem thông tin chi tiết một lớp học")
    public ResponseEntity<ClassroomResponse> getClassroomById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(classroomService.getClassroomById(id, principal.getId()));
    }

    @PostMapping("/join")
    @Operation(summary = "Học sinh xin vào lớp bằng mã tham gia (gửi yêu cầu chờ duyệt)")
    public ResponseEntity<ClassroomStudentResponse> joinClassroom(
            @Valid @RequestBody JoinClassroomRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(classroomService.requestToJoinByCode(request, principal.getId()));
    }

    @PutMapping("/{id}/requests/{studentId}/approve")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Giáo viên duyệt yêu cầu xin vào lớp của học sinh")
    public ResponseEntity<ClassroomStudentResponse> approveStudentRequest(
            @PathVariable UUID id,
            @PathVariable UUID studentId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(classroomService.approveStudentRequest(id, studentId, principal.getId()));
    }

    @PutMapping("/{id}/requests/{studentId}/reject")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Giáo viên từ chối yêu cầu xin vào lớp của học sinh")
    public ResponseEntity<MessageResponse> rejectStudentRequest(
            @PathVariable UUID id,
            @PathVariable UUID studentId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        classroomService.rejectStudentRequest(id, studentId, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã từ chối yêu cầu tham gia lớp học."));
    }

    @PostMapping("/{id}/invite")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Giáo viên mời học sinh vào lớp bằng email")
    public ResponseEntity<ClassroomStudentResponse> inviteStudent(
            @PathVariable UUID id,
            @Valid @RequestBody InviteStudentRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(classroomService.inviteStudent(id, request, principal.getId()));
    }

    @PutMapping("/{id}/invitations/accept")
    @Operation(summary = "Học sinh đồng ý lời mời tham gia lớp học")
    public ResponseEntity<ClassroomStudentResponse> acceptInvitation(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(classroomService.acceptInvitation(id, principal.getId()));
    }

    @PutMapping("/{id}/invitations/decline")
    @Operation(summary = "Học sinh từ chối lời mời tham gia lớp học")
    public ResponseEntity<MessageResponse> declineInvitation(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        classroomService.declineInvitation(id, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã từ chối lời mời tham gia lớp học."));
    }

    @GetMapping("/{id}/students")
    @Operation(summary = "Lấy danh sách học sinh của lớp học (lọc theo trạng thái)")
    public ResponseEntity<List<ClassroomStudentResponse>> getClassroomStudents(
            @PathVariable UUID id,
            @RequestParam(required = false) ClassEnrollmentStatus status,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(classroomService.getClassroomStudents(id, status, principal.getId()));
    }

    @DeleteMapping("/{id}/students/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Giáo viên xóa học sinh khỏi lớp học")
    public ResponseEntity<MessageResponse> removeStudent(
            @PathVariable UUID id,
            @PathVariable UUID studentId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        classroomService.removeStudent(id, studentId, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Đã xóa học sinh khỏi lớp học thành công."));
    }

    @PostMapping("/{id}/leave")
    @Operation(summary = "Học sinh tự rời khỏi lớp học")
    public ResponseEntity<MessageResponse> leaveClassroom(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        classroomService.leaveClassroom(id, principal.getId());
        return ResponseEntity.ok(MessageResponse.of("Bạn đã rời khỏi lớp học thành công."));
    }

    @GetMapping("/search-users")
    @Operation(summary = "Tìm kiếm đề xuất người dùng khi giáo viên gõ email mời vào lớp")
    public ResponseEntity<List<UserSuggestionResponse>> searchUsers(
            @RequestParam String query
    ) {
        return ResponseEntity.ok(classroomService.searchUsersForInvitation(query));
    }
}
