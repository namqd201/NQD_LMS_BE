package com.nqd.nqd_lms_be.service.freegrant;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.freegrant.*;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.EnrollmentStatus;
import com.nqd.nqd_lms_be.entity.enums.FreeQuotaRequestStatus;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseFreeGrantServiceImpl implements CourseFreeGrantService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseFreeQuotaRequestRepository courseFreeQuotaRequestRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final KafkaNotificationProducer kafkaNotificationProducer;

    @Override
    @Transactional(readOnly = true)
    public CourseFreeGrantSummaryResponse getFreeGrantSummary(UUID courseId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseTeacherAccess(course, teacherId);

        int totalQuota = course.getFreeGrantQuota() != null ? course.getFreeGrantQuota() : 10;
        long usedCount = courseEnrollmentRepository.countByCourseIdAndIsFreeGrantTrueAndIsDeletedFalse(courseId);
        int usedQuota = (int) usedCount;
        int remainingQuota = Math.max(0, totalQuota - usedQuota);

        List<CourseEnrollment> freeEnrollments = courseEnrollmentRepository.findFreeGrantsByCourseId(courseId);
        List<TeacherFreeGrantResponse> grantResponses = freeEnrollments.stream()
                .map(this::toGrantResponse)
                .collect(Collectors.toList());

        List<CourseFreeQuotaRequest> requests = courseFreeQuotaRequestRepository
                .findByCourseIdAndTeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(courseId, teacherId);
        List<CourseFreeQuotaRequestResponse> requestResponses = requests.stream()
                .map(this::toQuotaRequestResponse)
                .collect(Collectors.toList());

        return CourseFreeGrantSummaryResponse.builder()
                .courseId(course.getId())
                .courseName(course.getName())
                .totalQuota(totalQuota)
                .usedQuota(usedQuota)
                .remainingQuota(remainingQuota)
                .grants(grantResponses)
                .quotaRequests(requestResponses)
                .build();
    }

    @Override
    @Transactional
    public TeacherFreeGrantResponse addFreeGrant(UUID courseId, UUID teacherId, AddFreeGrantRequest request) {
        Course course = courseRepository.findById(courseId)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseTeacherAccess(course, teacherId);

        int totalQuota = course.getFreeGrantQuota() != null ? course.getFreeGrantQuota() : 10;
        long usedCount = courseEnrollmentRepository.countByCourseIdAndIsFreeGrantTrueAndIsDeletedFalse(courseId);
        if (usedCount >= totalQuota) {
            throw new IllegalArgumentException("Khóa học đã sử dụng hết " + totalQuota + " suất học viên miễn phí. Vui lòng bấm 'Yêu cầu cấp thêm suất miễn phí' để gửi Admin xét duyệt.");
        }

        String targetEmail = request.getEmail().trim().toLowerCase();
        User student = userRepository.findByEmail(targetEmail)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản học sinh nào với email: " + request.getEmail()));

        if (student.getId().equals(teacherId)) {
            throw new IllegalArgumentException("Giáo viên không thể tự tặng khóa học cho chính mình.");
        }

        Optional<CourseEnrollment> existingOpt = courseEnrollmentRepository.findByCourseIdAndStudentId(courseId, student.getId());
        CourseEnrollment enrollment;

        User teacher = userRepository.findById(teacherId).orElse(null);

        if (existingOpt.isPresent()) {
            enrollment = existingOpt.get();
            if (!enrollment.getIsDeleted() && enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
                throw new IllegalArgumentException("Học sinh với email \"" + targetEmail + "\" đã tham gia khóa học này rồi.");
            }
            enrollment.setIsDeleted(false);
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollment.setIsFreeGrant(true);
            enrollment.setGrantedBy(teacher);
            enrollment.setEnrolledAt(LocalDateTime.now());
        } else {
            enrollment = CourseEnrollment.builder()
                    .course(course)
                    .student(student)
                    .status(EnrollmentStatus.ENROLLED)
                    .enrolledAt(LocalDateTime.now())
                    .isFreeGrant(true)
                    .grantedBy(teacher)
                    .build();
        }

        CourseEnrollment saved = courseEnrollmentRepository.save(enrollment);

        // Update enrollment count
        course.setEnrollmentCount((course.getEnrollmentCount() != null ? course.getEnrollmentCount() : 0) + 1);
        courseRepository.save(course);

        // Notify student
        try {
            String teacherName = teacher != null ? teacher.getFullName() : "Giáo viên";
            kafkaNotificationProducer.sendNotification(
                    student.getId(),
                    "COURSE_FREE_GRANT",
                    "Bạn được tặng khóa học miễn phí!",
                    "Giáo viên " + teacherName + " đã tặng bạn khóa học \"" + course.getName() + "\". Chúc bạn học tập tốt!",
                    "/courses/" + course.getId()
            );
        } catch (Exception ex) {
            log.warn("Failed to send free grant notification to student: {}", ex.getMessage());
        }

        return toGrantResponse(saved);
    }

    @Override
    @Transactional
    public CourseFreeQuotaRequestResponse requestQuota(UUID courseId, UUID teacherId, CreateQuotaRequest request) {
        Course course = courseRepository.findById(courseId)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseTeacherAccess(course, teacherId);

        boolean hasPending = courseFreeQuotaRequestRepository.existsByCourseIdAndTeacherIdAndStatusAndIsDeletedFalse(
                courseId, teacherId, FreeQuotaRequestStatus.PENDING
        );
        if (hasPending) {
            throw new IllegalArgumentException("Khóa học này đang có một yêu cầu xin cấp thêm suất chờ Admin phê duyệt.");
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        CourseFreeQuotaRequest quotaRequest = CourseFreeQuotaRequest.builder()
                .course(course)
                .teacher(teacher)
                .requestedQuota(request.getRequestedQuota())
                .reason(request.getReason().trim())
                .status(FreeQuotaRequestStatus.PENDING)
                .build();

        CourseFreeQuotaRequest saved = courseFreeQuotaRequestRepository.save(quotaRequest);

        // Notify Admins
        notifyAdminsAboutQuotaRequest(saved);

        return toQuotaRequestResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseFreeQuotaRequestResponse> getAdminQuotaRequests(FreeQuotaRequestStatus status, Pageable pageable) {
        Page<CourseFreeQuotaRequest> page;
        if (status != null) {
            page = courseFreeQuotaRequestRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(status, pageable);
        } else {
            page = courseFreeQuotaRequestRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
        }
        return page.map(this::toQuotaRequestResponse);
    }

    @Override
    @Transactional
    public CourseFreeQuotaRequestResponse reviewQuotaRequest(UUID requestId, ReviewQuotaRequest request, String adminEmail) {
        CourseFreeQuotaRequest quotaReq = courseFreeQuotaRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu cấp thêm suất"));

        if (quotaReq.getStatus() != FreeQuotaRequestStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý trước đó với trạng thái: " + quotaReq.getStatus());
        }

        quotaReq.setStatus(request.getStatus());
        quotaReq.setAdminNote(request.getAdminNote() != null ? request.getAdminNote().trim() : null);
        quotaReq.setReviewedBy(adminEmail);
        quotaReq.setReviewedAt(LocalDateTime.now());

        Course course = quotaReq.getCourse();
        User teacher = quotaReq.getTeacher();

        if (request.getStatus() == FreeQuotaRequestStatus.APPROVED) {
            int currentQuota = course.getFreeGrantQuota() != null ? course.getFreeGrantQuota() : 10;
            course.setFreeGrantQuota(currentQuota + quotaReq.getRequestedQuota());
            courseRepository.save(course);

            // Notify Teacher about approval
            if (teacher != null) {
                try {
                    kafkaNotificationProducer.sendNotification(
                            teacher.getId(),
                            "QUOTA_REQUEST_APPROVED",
                            "Yêu cầu cấp thêm suất miễn phí đã được duyệt",
                            "Admin đã phê duyệt cấp thêm " + quotaReq.getRequestedQuota() + " suất học viên miễn phí cho khóa học \"" + course.getName() + "\". Tổng hạn mức hiện tại: " + course.getFreeGrantQuota() + " suất.",
                            "/teacher/courses/" + course.getId()
                    );
                } catch (Exception e) {
                    log.warn("Failed to notify teacher about quota approval: {}", e.getMessage());
                }
            }
        } else if (request.getStatus() == FreeQuotaRequestStatus.REJECTED) {
            // Notify Teacher about rejection
            if (teacher != null) {
                try {
                    String reasonText = quotaReq.getAdminNote() != null && !quotaReq.getAdminNote().isBlank()
                            ? quotaReq.getAdminNote()
                            : "Yêu cầu chưa phù hợp chính sách của hệ thống.";
                    kafkaNotificationProducer.sendNotification(
                            teacher.getId(),
                            "QUOTA_REQUEST_REJECTED",
                            "Yêu cầu cấp thêm suất miễn phí bị từ chối",
                            "Admin đã từ chối yêu cầu cấp thêm suất miễn phí cho khóa học \"" + course.getName() + "\". Lý do: " + reasonText,
                            "/teacher/courses/" + course.getId()
                    );
                } catch (Exception e) {
                    log.warn("Failed to notify teacher about quota rejection: {}", e.getMessage());
                }
            }
        }

        CourseFreeQuotaRequest saved = courseFreeQuotaRequestRepository.save(quotaReq);
        return toQuotaRequestResponse(saved);
    }

    private void verifyCourseTeacherAccess(Course course, UUID teacherId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        boolean isCreator = course.getCreator() != null && course.getCreator().getId().equals(teacherId);
        boolean isAssigned = courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), teacherId);
        if (!isCreator && !isAssigned) {
            throw new ForbiddenOperationException("Bạn không có quyền quản lý khóa học này.");
        }
    }

    private void notifyAdminsAboutQuotaRequest(CourseFreeQuotaRequest req) {
        try {
            Set<UUID> notifiedAdminIds = new HashSet<>();
            List<User> admins = new ArrayList<>();
            List<User> roleAdmins = userRoleRepository.findUsersByRoleName("ROLE_ADMIN");
            if (roleAdmins != null) admins.addAll(roleAdmins);
            List<User> directAdmins = userRoleRepository.findUsersByRoleName("ADMIN");
            if (directAdmins != null) admins.addAll(directAdmins);

            String title = "Yêu cầu cấp thêm suất học viên miễn phí";
            String teacherName = req.getTeacher() != null ? req.getTeacher().getFullName() : "Giảng viên";
            String courseName = req.getCourse() != null ? req.getCourse().getName() : "Khóa học";
            String body = String.format("Giáo viên %s vừa yêu cầu cấp thêm %d suất miễn phí cho khóa học \"%s\". Lý do: %s",
                    teacherName, req.getRequestedQuota(), courseName, req.getReason());

            for (User admin : admins) {
                if (admin != null && admin.getId() != null && notifiedAdminIds.add(admin.getId())) {
                    kafkaNotificationProducer.sendNotification(
                            admin.getId(),
                            "FREE_QUOTA_REQUESTED",
                            title,
                            body,
                            "/admin/courses"
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send quota request notification to admins: {}", e.getMessage());
        }
    }

    private TeacherFreeGrantResponse toGrantResponse(CourseEnrollment ce) {
        User student = ce.getStudent();
        Course course = ce.getCourse();
        return TeacherFreeGrantResponse.builder()
                .enrollmentId(ce.getId())
                .studentId(student != null ? student.getId() : null)
                .studentName(student != null ? student.getFullName() : null)
                .studentEmail(student != null ? student.getEmail() : null)
                .grantedAt(ce.getEnrolledAt() != null ? ce.getEnrolledAt() : ce.getCreatedAt())
                .courseId(course != null ? course.getId() : null)
                .courseName(course != null ? course.getName() : null)
                .build();
    }

    private CourseFreeQuotaRequestResponse toQuotaRequestResponse(CourseFreeQuotaRequest req) {
        Course course = req.getCourse();
        User teacher = req.getTeacher();
        return CourseFreeQuotaRequestResponse.builder()
                .id(req.getId())
                .courseId(course != null ? course.getId() : null)
                .courseName(course != null ? course.getName() : null)
                .courseCode(course != null ? course.getCode() : null)
                .teacherId(teacher != null ? teacher.getId() : null)
                .teacherName(teacher != null ? teacher.getFullName() : null)
                .teacherEmail(teacher != null ? teacher.getEmail() : null)
                .requestedQuota(req.getRequestedQuota())
                .reason(req.getReason())
                .status(req.getStatus())
                .adminNote(req.getAdminNote())
                .reviewedBy(req.getReviewedBy())
                .reviewedAt(req.getReviewedAt())
                .createdAt(req.getCreatedAt())
                .build();
    }
}
