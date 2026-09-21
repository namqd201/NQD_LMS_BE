package com.nqd.nqd_lms_be.service.student;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.student.StudentCourseResponse;
import com.nqd.nqd_lms_be.entity.Course;
import com.nqd.nqd_lms_be.entity.CourseEnrollment;
import com.nqd.nqd_lms_be.entity.User;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.EnrollmentStatus;
import com.nqd.nqd_lms_be.repository.CourseEnrollmentRepository;
import com.nqd.nqd_lms_be.repository.CourseRepository;
import com.nqd.nqd_lms_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
public class StudentCourseServiceImpl implements StudentCourseService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final UserRepository userRepository;
    private final com.nqd.nqd_lms_be.repository.EntitlementRepository entitlementRepository;
    private final com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService membershipEntitlementService;
    private final com.nqd.nqd_lms_be.finance.service.TeacherFinanceService teacherFinanceService;
    private final com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer kafkaNotificationProducer;

    @Override
    @Transactional(readOnly = true)
    public List<StudentCourseResponse> getPublishedCourses(UUID studentId) {
        return courseRepository.findByStatus(CourseStatus.ACTIVE).stream()
                .map(course -> mapToStudentCourseResponse(course, studentId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentCourseResponse> getEnrolledCourses(UUID studentId) {
        return courseEnrollmentRepository.findByStudentId(studentId).stream()
                .map(enrollment -> mapToStudentCourseResponse(enrollment.getCourse(), studentId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void enrollCourse(UUID courseId, UUID studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        if (course.getStatus() != CourseStatus.ACTIVE && course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ForbiddenOperationException("Cannot enroll in an inactive course");
        }

        if (course.getCreator() != null && course.getCreator().getId().equals(studentId)) {
            log.info("User {} is creator of course {}, no need to enroll", studentId, courseId);
            return;
        }

        if (courseEnrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            log.info("Student {} already enrolled in course {}", studentId, courseId);
            return;
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));

        if (course.isPaid()) {
            boolean isUltra = membershipEntitlementService.hasFeature(studentId, com.nqd.nqd_lms_be.entity.enums.FeatureKey.ULTRA_UNLIMITED_COURSES);
            boolean hasCourseAccess = entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                    studentId, com.nqd.nqd_lms_be.entity.enums.EntitlementType.COURSE_ACCESS, courseId, com.nqd.nqd_lms_be.entity.enums.EntitlementStatus.ACTIVE
            ).isPresent();

            if (isUltra) {
                // Enforce fair-use limits (10 courses/day, 100 courses/month)
                membershipEntitlementService.enforceAndConsumeUsage(studentId, com.nqd.nqd_lms_be.entity.enums.FeatureKey.ULTRA_COURSE_ENROLLMENT, 1);

                // Record 20% royalty for teacher from Ultra fund
                if (course.getEffectivePrice() != null && course.getEffectivePrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    java.math.BigDecimal royaltyAmount = course.getEffectivePrice().multiply(new java.math.BigDecimal("0.20")).setScale(2, java.math.RoundingMode.HALF_UP);
                    teacherFinanceService.recordUltraRoyaltyEarning(course, student, royaltyAmount);
                }
            } else if (!hasCourseAccess) {
                throw new ForbiddenOperationException("Khóa học này có phí. Vui lòng mua khóa học hoặc nâng cấp gói Ultra để tham gia.");
            }
        }

        boolean isPrivate = Boolean.TRUE.equals(course.getIsPrivate());
        EnrollmentStatus initialStatus = isPrivate
                ? EnrollmentStatus.PENDING
                : EnrollmentStatus.ENROLLED;

        CourseEnrollment enrollment = CourseEnrollment.builder()
                .course(course)
                .student(student)
                .status(initialStatus)
                .enrolledAt(LocalDateTime.now())
                .build();

        courseEnrollmentRepository.save(enrollment);
        log.info("Student {} requested/enrolled in course {} with status {}", student.getEmail(), course.getName(), initialStatus);

        // Send notifications via Kafka
        if (isPrivate) {
            // Student notification
            kafkaNotificationProducer.sendNotification(
                    studentId,
                    "ENROLLMENT_PENDING",
                    "Yêu cầu tham gia đã được gửi",
                    String.format("Yêu cầu xin vào khóa học '%s' đang chờ giáo viên phê duyệt.", course.getName()),
                    "/courses/" + course.getId()
            );

            // Teacher notification
            if (course.getCreator() != null) {
                kafkaNotificationProducer.sendNotification(
                        course.getCreator().getId(),
                        "ENROLLMENT_REQUEST",
                        "Yêu cầu tham gia lớp mới",
                        String.format("Học sinh %s (%s) đã gửi yêu cầu tham gia khóa học '%s'.",
                                student.getFullName(), student.getEmail(), course.getName()),
                        "/teacher/courses/" + course.getId()
                );
            }
        } else {
            // Student notification
            kafkaNotificationProducer.sendNotification(
                    studentId,
                    "ENROLLMENT_SUCCESS",
                    "Đăng ký khóa học thành công",
                    String.format("Bạn đã tham gia khóa học '%s'. Chúc bạn có trải nghiệm học tập tuyệt vời!", course.getName()),
                    "/courses/" + course.getId()
            );

            // Teacher notification
            if (course.getCreator() != null) {
                kafkaNotificationProducer.sendNotification(
                        course.getCreator().getId(),
                        "STUDENT_JOINED",
                        "Học sinh mới tham gia",
                        String.format("Học sinh %s (%s) vừa đăng ký khóa học '%s'.",
                                student.getFullName(), student.getEmail(), course.getName()),
                        "/teacher/courses/" + course.getId()
                );
            }
        }
    }

    private StudentCourseResponse mapToStudentCourseResponse(Course course, UUID studentId) {
        Optional<CourseEnrollment> enrollment = courseEnrollmentRepository.findByCourseIdAndStudentId(course.getId(), studentId);
        boolean isOwner = course.getCreator() != null && course.getCreator().getId().equals(studentId);

        return StudentCourseResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .code(course.getCode())
                .description(course.getDescription())
                .gradeLevel(course.getGradeLevel())
                .thumbnailUrl(course.getThumbnailUrl())
                .subjectName(course.getSubject() != null ? course.getSubject().getName() : null)
                .status(course.getStatus())
                .isPrivate(Boolean.TRUE.equals(course.getIsPrivate()))
                .creatorId(course.getCreator() != null ? course.getCreator().getId() : null)
                .creatorName(course.getCreator() != null ? course.getCreator().getFullName() : null)
                .isOwner(isOwner)
                .isEnrolled(enrollment.isPresent() || isOwner)
                .enrollmentStatus(enrollment.map(CourseEnrollment::getStatus).orElse(null))
                .build();
    }
}
