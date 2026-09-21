package com.nqd.nqd_lms_be.service.admin;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.admin.AdminCourseDisableRequest;
import com.nqd.nqd_lms_be.dto.admin.AdminCourseResponse;
import com.nqd.nqd_lms_be.entity.Course;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.repository.CourseEnrollmentRepository;
import com.nqd.nqd_lms_be.repository.CourseRepository;
import com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseServiceImpl implements AdminCourseService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final KafkaNotificationProducer kafkaNotificationProducer;

    @Override
    @Transactional(readOnly = true)
    public List<AdminCourseResponse> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCourseResponse getCourseById(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        return mapToResponse(course);
    }

    @Override
    @Transactional
    public AdminCourseResponse disableCourse(UUID id, AdminCourseDisableRequest request, UUID adminId) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));

        course.setIsDisabled(true);
        course.setDisabledReason(request.getReason());
        course.setStatus(CourseStatus.ARCHIVED);

        course = courseRepository.save(course);
        log.info("Admin {} disabled course {} with reason: {}", adminId, id, request.getReason());

        // Send Kafka notification to course creator
        if (course.getCreator() != null) {
            String title = "Khóa học của bạn đã bị vô hiệu hóa";
            String body = String.format("Khóa học '%s' đã bị Quản trị viên vô hiệu hóa. Lý do: %s",
                    course.getName(), request.getReason());
            String linkUrl = "/teacher/courses/" + course.getId();

            kafkaNotificationProducer.sendNotification(
                    course.getCreator().getId(),
                    "COURSE_DISABLED",
                    title,
                    body,
                    linkUrl
            );
        }

        return mapToResponse(course);
    }

    @Override
    @Transactional
    public AdminCourseResponse enableCourse(UUID id, UUID adminId) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));

        course.setIsDisabled(false);
        course.setDisabledReason(null);
        course.setStatus(CourseStatus.ACTIVE);

        course = courseRepository.save(course);
        log.info("Admin {} re-enabled course {}", adminId, id);

        // Send Kafka notification to course creator
        if (course.getCreator() != null) {
            String title = "Khóa học của bạn đã được kích hoạt lại";
            String body = String.format("Khóa học '%s' đã được Quản trị viên kích hoạt trở lại và sẵn sàng cho học sinh.",
                    course.getName());
            String linkUrl = "/teacher/courses/" + course.getId();

            kafkaNotificationProducer.sendNotification(
                    course.getCreator().getId(),
                    "COURSE_ENABLED",
                    title,
                    body,
                    linkUrl
            );
        }

        return mapToResponse(course);
    }

    @Override
    @Transactional
    public void deleteCourse(UUID id, UUID adminId) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));

        // Rule: Admin CANNOT delete course that was not created by this admin
        if (course.getCreator() == null || !course.getCreator().getId().equals(adminId)) {
            throw new ForbiddenOperationException("Quản trị viên không thể xóa khóa học do giáo viên khác tạo. Bạn có thể vô hiệu hóa (Disable) khóa học kèm lý do giải thích.");
        }

        // Rule: Cannot delete course if students are enrolled
        long enrolledCount = courseEnrollmentRepository.countByCourseId(id);
        if (enrolledCount > 0) {
            throw new ForbiddenOperationException("Không thể xóa khóa học đang có " + enrolledCount + " học sinh đăng ký/theo học.");
        }

        courseRepository.delete(course);
        log.info("Admin {} deleted self-created course {}", adminId, id);
    }

    private AdminCourseResponse mapToResponse(Course course) {
        long enrolledCount = courseEnrollmentRepository.countByCourseId(course.getId());

        return AdminCourseResponse.builder()
                .id(course.getId())
                .subjectId(course.getSubject() != null ? course.getSubject().getId() : null)
                .subjectName(course.getSubject() != null ? course.getSubject().getName() : null)
                .name(course.getName())
                .code(course.getCode())
                .description(course.getDescription())
                .gradeLevel(course.getGradeLevel())
                .thumbnailUrl(course.getThumbnailUrl())
                .status(course.getStatus())
                .isPrivate(Boolean.TRUE.equals(course.getIsPrivate()))
                .isDisabled(Boolean.TRUE.equals(course.getIsDisabled()))
                .disabledReason(course.getDisabledReason())
                .pricingType(course.getPricingType())
                .price(course.getPrice())
                .salePrice(course.getSalePrice())
                .currency(course.getCurrency())
                .publishedAt(course.getPublishedAt())
                .rejectReason(course.getRejectReason())
                .averageRating(course.getAverageRating())
                .reviewCount(course.getReviewCount())
                .creatorId(course.getCreator() != null ? course.getCreator().getId() : null)
                .creatorName(course.getCreator() != null ? course.getCreator().getFullName() : null)
                .creatorEmail(course.getCreator() != null ? course.getCreator().getEmail() : null)
                .enrolledStudentsCount(enrolledCount)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
