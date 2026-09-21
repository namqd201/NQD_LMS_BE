package com.nqd.nqd_lms_be.service.course;

import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.teacher.TeacherCourseResponse;
import com.nqd.nqd_lms_be.entity.Course;
import com.nqd.nqd_lms_be.entity.Product;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.ProductStatus;
import com.nqd.nqd_lms_be.entity.enums.ProductType;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseWorkflowServiceImpl implements CourseWorkflowService {

    private final CourseRepository courseRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final ChapterRepository chapterRepository;
    private final ProductRepository productRepository;
    private final KafkaNotificationProducer kafkaNotificationProducer;

    @Override
    @Transactional
    public TeacherCourseResponse submitForReview(UUID courseId, UUID teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        verifyCourseOwnership(course, teacherId);

        if (course.getStatus() == CourseStatus.PUBLISHED || course.getStatus() == CourseStatus.ACTIVE) {
            throw new IllegalStateException("Khóa học đã được xuất bản công khai.");
        }
        if (course.getStatus() == CourseStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Khóa học đang trong quá trình xét duyệt.");
        }

        long chapterCount = chapterRepository.countByCourseId(courseId);
        if (chapterCount == 0) {
            throw new IllegalStateException("Khóa học phải có ít nhất một chương học trước khi gửi duyệt.");
        }

        course.setStatus(CourseStatus.PENDING_REVIEW);
        course.setRejectReason(null);
        course = courseRepository.save(course);
        log.info("Course {} submitted for review by teacher {}", courseId, teacherId);

        return mapToCourseResponse(course);
    }

    @Override
    @Transactional
    public TeacherCourseResponse approveCourse(UUID courseId, UUID adminId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        if (!SecurityUtils.isAdmin()) {
            throw new ForbiddenOperationException("Chỉ Quản trị viên mới có quyền phê duyệt khóa học.");
        }

        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(LocalDateTime.now());
        course.setRejectReason(null);
        course = courseRepository.save(course);

        // Sync product entity for billing
        syncCourseProduct(course);

        log.info("Course {} approved and published by admin {}", courseId, adminId);

        // Send notification to course creator
        if (course.getCreator() != null) {
            kafkaNotificationProducer.sendNotification(
                    course.getCreator().getId(),
                    "COURSE_APPROVED",
                    "Khóa học đã được phê duyệt!",
                    String.format("Khóa học '%s' của bạn đã được Quản trị viên phê duyệt và xuất bản lên Marketplace.", course.getName()),
                    "/teacher/courses/" + course.getId()
            );
        }

        return mapToCourseResponse(course);
    }

    @Override
    @Transactional
    public TeacherCourseResponse rejectCourse(UUID courseId, UUID adminId, String reason) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        if (!SecurityUtils.isAdmin()) {
            throw new ForbiddenOperationException("Chỉ Quản trị viên mới có quyền từ chối khóa học.");
        }

        String finalReason = (reason != null && !reason.isBlank()) ? reason : "Nội dung khóa học chưa đạt yêu cầu kiểm duyệt.";
        course.setStatus(CourseStatus.REJECTED);
        course.setRejectReason(finalReason);
        course = courseRepository.save(course);

        log.info("Course {} rejected by admin {} with reason: {}", courseId, adminId, finalReason);

        // Send notification to course creator
        if (course.getCreator() != null) {
            kafkaNotificationProducer.sendNotification(
                    course.getCreator().getId(),
                    "COURSE_REJECTED",
                    "Khóa học cần chỉnh sửa thêm",
                    String.format("Khóa học '%s' chưa được phê duyệt. Lý do: %s", course.getName(), finalReason),
                    "/teacher/courses/" + course.getId()
            );
        }

        return mapToCourseResponse(course);
    }

    private void syncCourseProduct(Course course) {
        Optional<Product> productOpt = productRepository.findByTargetEntityIdAndProductTypeAndIsDeletedFalse(
                course.getId(), ProductType.COURSE
        );

        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setTitle(course.getName());
            product.setDescription(course.getDescription());
            product.setBasePrice(course.getEffectivePrice());
            product.setCurrency(course.getCurrency() != null ? course.getCurrency() : "VND");
            product.setIsFree(!course.isPaid());
            product.setStatus(ProductStatus.PUBLISHED);
            product.setThumbnailUrl(course.getThumbnailUrl());
            productRepository.save(product);
        } else {
            Product newProduct = Product.builder()
                    .code("PRD-CRS-" + course.getCode())
                    .title(course.getName())
                    .description(course.getDescription())
                    .productType(ProductType.COURSE)
                    .targetEntityId(course.getId())
                    .basePrice(course.getEffectivePrice())
                    .currency(course.getCurrency() != null ? course.getCurrency() : "VND")
                    .isFree(!course.isPaid())
                    .status(ProductStatus.PUBLISHED)
                    .thumbnailUrl(course.getThumbnailUrl())
                    .build();
            productRepository.save(newProduct);
        }
    }

    private void verifyCourseOwnership(Course course, UUID teacherId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        boolean isCreator = course.getCreator() != null && course.getCreator().getId().equals(teacherId);
        boolean isAssigned = courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), teacherId);

        if (!isCreator && !isAssigned) {
            throw new ForbiddenOperationException("Bạn không có quyền quản lý khóa học này.");
        }
    }

    private TeacherCourseResponse mapToCourseResponse(Course course) {
        return TeacherCourseResponse.builder()
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
                .pricingType(course.getPricingType())
                .price(course.getPrice())
                .salePrice(course.getSalePrice())
                .currency(course.getCurrency())
                .publishedAt(course.getPublishedAt())
                .rejectReason(course.getRejectReason())
                .averageRating(course.getAverageRating())
                .reviewCount(course.getReviewCount())
                .enrollmentCount(course.getEnrollmentCount())
                .creatorId(course.getCreator() != null ? course.getCreator().getId() : null)
                .creatorName(course.getCreator() != null ? course.getCreator().getFullName() : null)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .isDeleted(course.getIsDeleted())
                .deletedAt(course.getDeletedAt())
                .deletedBy(course.getDeletedBy())
                .build();
    }
}
