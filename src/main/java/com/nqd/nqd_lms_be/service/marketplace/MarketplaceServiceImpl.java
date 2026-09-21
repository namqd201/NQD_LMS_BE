package com.nqd.nqd_lms_be.service.marketplace;

import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.dto.marketplace.*;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.CoursePricingType;
import com.nqd.nqd_lms_be.entity.enums.CourseStatus;
import com.nqd.nqd_lms_be.entity.enums.EnrollmentStatus;
import com.nqd.nqd_lms_be.entity.enums.EntitlementStatus;
import com.nqd.nqd_lms_be.entity.enums.EntitlementType;
import com.nqd.nqd_lms_be.entity.enums.FeatureKey;
import com.nqd.nqd_lms_be.entity.enums.LessonProgressStatus;
import com.nqd.nqd_lms_be.repository.*;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceServiceImpl implements MarketplaceService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final EntitlementRepository entitlementRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final UserRepository userRepository;
    private final com.nqd.nqd_lms_be.service.moderation.ContentModerationService contentModerationService;
    private final com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService membershipEntitlementService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MarketplaceCourseCardResponse> searchCourses(MarketplaceFilterRequest request, Pageable pageable) {
        Specification<Course> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Only published & non-deleted & non-disabled courses
            predicates.add(cb.or(
                    cb.equal(root.get("status"), CourseStatus.PUBLISHED),
                    cb.equal(root.get("status"), CourseStatus.ACTIVE)
            ));
            predicates.add(cb.or(
                    cb.isNull(root.get("isDeleted")),
                    cb.isFalse(root.get("isDeleted"))
            ));
            predicates.add(cb.or(
                    cb.isNull(root.get("isDisabled")),
                    cb.isFalse(root.get("isDisabled"))
            ));

            if (request != null) {
                // Keyword search
                if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                    String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), pattern),
                            cb.like(cb.lower(root.get("description")), pattern),
                            cb.like(cb.lower(root.get("code")), pattern)
                    ));
                }

                // Subject
                if (request.getSubjectId() != null) {
                    predicates.add(cb.equal(root.get("subject").get("id"), request.getSubjectId()));
                }

                // Grade
                if (request.getGradeLevel() != null && !request.getGradeLevel().isBlank()) {
                    predicates.add(cb.equal(root.get("gradeLevel"), request.getGradeLevel().trim()));
                }

                // Teacher
                if (request.getTeacherId() != null) {
                    predicates.add(cb.equal(root.get("creator").get("id"), request.getTeacherId()));
                }

                // Free / Paid filter
                if (request.getIsFree() != null) {
                    if (Boolean.TRUE.equals(request.getIsFree())) {
                        predicates.add(cb.or(
                                cb.equal(root.get("pricingType"), CoursePricingType.FREE),
                                cb.isNull(root.get("price")),
                                cb.equal(root.get("price"), BigDecimal.ZERO)
                        ));
                    } else {
                        predicates.add(cb.and(
                                cb.equal(root.get("pricingType"), CoursePricingType.PAID),
                                cb.isNotNull(root.get("price")),
                                cb.greaterThan(root.get("price"), BigDecimal.ZERO)
                        ));
                    }
                }

                // Price range
                if (request.getMinPrice() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
                }
                if (request.getMaxPrice() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
                }

                // Rating
                if (request.getMinRating() != null && request.getMinRating() > 0) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), request.getMinRating()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Determine sorting
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (request != null && request.getSortBy() != null) {
            switch (request.getSortBy().toLowerCase()) {
                case "newest" -> sort = Sort.by(Sort.Direction.DESC, "publishedAt", "createdAt");
                case "popular" -> sort = Sort.by(Sort.Direction.DESC, "enrollmentCount", "reviewCount");
                case "rating" -> sort = Sort.by(Sort.Direction.DESC, "averageRating");
                case "price_asc" -> sort = Sort.by(Sort.Direction.ASC, "price");
                case "price_desc" -> sort = Sort.by(Sort.Direction.DESC, "price");
            }
        }

        Pageable effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<Course> coursePage = courseRepository.findAll(spec, effectivePageable);

        return PageResponse.fromPage(coursePage, this::mapToCourseCardResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MarketplaceCourseDetailResponse getCourseDetail(UUID courseId, UUID currentUserId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        if (!course.isPublished()) {
            throw new ForbiddenOperationException("Khóa học chưa được xuất bản công khai.");
        }

        boolean isOwner = currentUserId != null && course.getCreator() != null && course.getCreator().getId().equals(currentUserId);
        boolean isEnrolled = isOwner;
        boolean hasPurchased = isOwner;
        boolean isUltraMember = false;
        EnrollmentStatus enrollmentStatus = null;

        if (currentUserId != null && !isOwner) {
            isUltraMember = membershipEntitlementService.hasFeature(currentUserId, FeatureKey.ULTRA_UNLIMITED_COURSES);
            if (isUltraMember) {
                hasPurchased = true;
                isEnrolled = true;
            }

            Optional<CourseEnrollment> enrollmentOpt = courseEnrollmentRepository.findByCourseIdAndStudentId(courseId, currentUserId);
            if (enrollmentOpt.isPresent()) {
                enrollmentStatus = enrollmentOpt.get().getStatus();
                if (enrollmentStatus == EnrollmentStatus.ENROLLED) {
                    isEnrolled = true;
                }
            }

            if (!hasPurchased) {
                hasPurchased = entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                        currentUserId, EntitlementType.COURSE_ACCESS, courseId, EntitlementStatus.ACTIVE
                ).isPresent();

                if (hasPurchased) {
                    isEnrolled = true;
                }
            }
        }

        BigDecimal proDiscountPrice = null;
        if (course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            proDiscountPrice = course.getPrice().multiply(new BigDecimal("0.80")).setScale(2, java.math.RoundingMode.HALF_UP);
        }

        // Chapters & Preview info
        List<Chapter> chapters = chapterRepository.findByCourseIdOrderByDisplayOrderAsc(courseId);
        List<MarketplaceCourseDetailResponse.MarketplaceChapterResponse> chapterResponses = new ArrayList<>();

        for (Chapter ch : chapters) {
            boolean chapterPurchased = isOwner || isUltraMember || hasPurchased;
            if (!chapterPurchased && currentUserId != null) {
                chapterPurchased = entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                        currentUserId, EntitlementType.CHAPTER_ACCESS, ch.getId(), EntitlementStatus.ACTIVE
                ).isPresent();
            }

            List<Lesson> lessons = lessonRepository.findByChapterIdOrderByDisplayOrderAsc(ch.getId());
            boolean finalChapterPurchased = chapterPurchased;
            List<MarketplaceCourseDetailResponse.MarketplaceLessonSummaryResponse> lessonResponses = lessons.stream()
                    .map(l -> {
                        boolean lessonPurchased = finalChapterPurchased;
                        if (!lessonPurchased && currentUserId != null) {
                            lessonPurchased = entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                                    currentUserId, EntitlementType.LESSON_ACCESS, l.getId(), EntitlementStatus.ACTIVE
                            ).isPresent();
                        }
                        return MarketplaceCourseDetailResponse.MarketplaceLessonSummaryResponse.builder()
                                .id(l.getId())
                                .title(l.getTitle())
                                .slug(l.getSlug())
                                .summary(l.getSummary())
                                .displayOrder(l.getDisplayOrder())
                                .estimatedMinutes(l.getEstimatedMinutes())
                                .isPreview(Boolean.TRUE.equals(l.getIsPreview()))
                                .price(l.getPrice())
                                .isSellable(Boolean.TRUE.equals(l.getIsSellable()))
                                .isPurchased(lessonPurchased)
                                .build();
                    })
                    .collect(Collectors.toList());

            chapterResponses.add(MarketplaceCourseDetailResponse.MarketplaceChapterResponse.builder()
                    .id(ch.getId())
                    .title(ch.getTitle())
                    .description(ch.getDescription())
                    .displayOrder(ch.getDisplayOrder())
                    .price(ch.getPrice())
                    .isSellable(Boolean.TRUE.equals(ch.getIsSellable()))
                    .isPurchased(chapterPurchased)
                    .lessons(lessonResponses)
                    .build());
        }

        return MarketplaceCourseDetailResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .code(course.getCode())
                .description(course.getDescription())
                .gradeLevel(course.getGradeLevel())
                .thumbnailUrl(course.getThumbnailUrl())
                .subjectId(course.getSubject() != null ? course.getSubject().getId() : null)
                .subjectName(course.getSubject() != null ? course.getSubject().getName() : null)
                .status(course.getStatus())
                .pricingType(course.getPricingType())
                .price(course.getPrice())
                .salePrice(course.getSalePrice())
                .proDiscountPrice(proDiscountPrice)
                .currency(course.getCurrency())
                .isFree(!course.isPaid())
                .averageRating(course.getAverageRating())
                .reviewCount(course.getReviewCount())
                .enrollmentCount(course.getEnrollmentCount())
                .creatorId(course.getCreator() != null ? course.getCreator().getId() : null)
                .creatorName(course.getCreator() != null ? course.getCreator().getFullName() : null)
                .creatorAvatarUrl(course.getCreator() != null ? course.getCreator().getAvatarUrl() : null)
                .publishedAt(course.getPublishedAt())
                .isEnrolled(isEnrolled)
                .hasPurchased(hasPurchased)
                .isUltraMember(isUltraMember)
                .enrollmentStatus(enrollmentStatus)
                .isOwner(isOwner)
                .chapters(chapterResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseReviewResponse> getCourseReviews(UUID courseId, Pageable pageable) {
        Page<CourseReview> reviewPage = courseReviewRepository.findByCourseIdAndIsDeletedFalseOrderByCreatedAtDesc(courseId, pageable);
        return PageResponse.fromPage(reviewPage, this::mapToReviewResponse);
    }

    @Override
    @Transactional
    public CourseReviewResponse createCourseReview(UUID courseId, UUID userId, CreateCourseReviewRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Rule: User must have completed the course (or is creator/admin)
        boolean isOwner = course.getCreator() != null && course.getCreator().getId().equals(userId);
        boolean isAdmin = SecurityUtils.isAdmin();

        if (!isOwner && !isAdmin) {
            Optional<CourseEnrollment> enrollmentOpt = courseEnrollmentRepository.findByCourseIdAndStudentId(courseId, userId);
            boolean hasPurchased = entitlementRepository.findByUserIdAndEntitlementTypeAndTargetEntityIdAndStatusAndIsDeletedFalse(
                    userId, EntitlementType.COURSE_ACCESS, courseId, EntitlementStatus.ACTIVE
            ).isPresent();

            if (enrollmentOpt.isEmpty() && !hasPurchased) {
                throw new ForbiddenOperationException("Chỉ học viên đã tham gia khóa học mới được phép gửi đánh giá.");
            }

            boolean isCompletedStatus = enrollmentOpt.map(e -> e.getStatus() == EnrollmentStatus.COMPLETED).orElse(false);

            if (!isCompletedStatus) {
                long totalLessons = lessonRepository.countByCourseId(courseId);
                List<LessonProgress> progresses = lessonProgressRepository.findByStudentIdAndCourseId(userId, courseId);
                long completedLessons = progresses.stream()
                        .filter(p -> p.getStatus() == LessonProgressStatus.COMPLETED)
                        .count();

                boolean isAllLessonsCompleted = totalLessons == 0 || completedLessons >= totalLessons;
                if (!isAllLessonsCompleted) {
                    throw new ForbiddenOperationException("Bạn cần hoàn thành 100% khóa học để có thể gửi đánh giá.");
                }

                // Sync status to COMPLETED if all lessons are done
                enrollmentOpt.ifPresent(e -> {
                    if (e.getStatus() != EnrollmentStatus.COMPLETED) {
                        e.setStatus(EnrollmentStatus.COMPLETED);
                        e.setCompletedAt(LocalDateTime.now());
                        courseEnrollmentRepository.save(e);
                    }
                });
            }
        }

        // Rule: Only 1 review per user per course
        if (courseReviewRepository.existsByCourseIdAndUserIdAndIsDeletedFalse(courseId, userId)) {
            throw new IllegalStateException("Bạn đã đánh giá khóa học này rồi. Bạn có thể chỉnh sửa lại đánh giá hiện tại.");
        }

        // Content Moderation check
        if (request.getComment() != null && !request.getComment().isBlank()) {
            contentModerationService.validateContent(request.getComment(), "Nội dung nhận xét đánh giá");
        }

        CourseReview review = CourseReview.builder()
                .course(course)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        review = courseReviewRepository.save(review);
        log.info("User {} reviewed course {} with rating {}", user.getEmail(), courseId, request.getRating());

        // Recalculate average rating & review count on Course
        updateCourseRatingStats(course);

        return mapToReviewResponse(review);
    }

    @Override
    @Transactional
    public CourseReviewResponse updateCourseReview(UUID courseId, UUID reviewId, UUID userId, UpdateCourseReviewRequest request) {
        CourseReview review = courseReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseReview", reviewId));

        if (!review.getCourse().getId().equals(courseId)) {
            throw new IllegalArgumentException("Đánh giá không thuộc khóa học này.");
        }

        if (!review.getUser().getId().equals(userId) && !SecurityUtils.isAdmin()) {
            throw new ForbiddenOperationException("Bạn không có quyền chỉnh sửa đánh giá này.");
        }

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null) {
            if (!request.getComment().isBlank()) {
                contentModerationService.validateContent(request.getComment(), "Nội dung nhận xét đánh giá");
            }
            review.setComment(request.getComment());
        }

        review = courseReviewRepository.save(review);
        updateCourseRatingStats(review.getCourse());

        log.info("User {} updated review {} for course {}", userId, reviewId, courseId);
        return mapToReviewResponse(review);
    }

    @Override
    @Transactional
    public void deleteCourseReview(UUID courseId, UUID reviewId, UUID userId) {
        CourseReview review = courseReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseReview", reviewId));

        if (!review.getCourse().getId().equals(courseId)) {
            throw new IllegalArgumentException("Đánh giá không thuộc khóa học này.");
        }

        if (!review.getUser().getId().equals(userId) && !SecurityUtils.isAdmin()) {
            throw new ForbiddenOperationException("Bạn không có quyền xóa đánh giá này.");
        }

        review.setIsDeleted(true);
        courseReviewRepository.save(review);
        updateCourseRatingStats(review.getCourse());

        log.info("User {} deleted review {} for course {}", userId, reviewId, courseId);
    }

    private void updateCourseRatingStats(Course course) {
        Double avgRating = courseReviewRepository.getAverageRatingByCourseId(course.getId());
        long count = courseReviewRepository.countByCourseIdAndIsDeletedFalse(course.getId());

        course.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        course.setReviewCount((int) count);
        courseRepository.save(course);
    }

    private MarketplaceCourseCardResponse mapToCourseCardResponse(Course course) {
        return MarketplaceCourseCardResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .code(course.getCode())
                .description(course.getDescription())
                .gradeLevel(course.getGradeLevel())
                .thumbnailUrl(course.getThumbnailUrl())
                .subjectId(course.getSubject() != null ? course.getSubject().getId() : null)
                .subjectName(course.getSubject() != null ? course.getSubject().getName() : null)
                .status(course.getStatus())
                .pricingType(course.getPricingType())
                .price(course.getPrice())
                .salePrice(course.getSalePrice())
                .currency(course.getCurrency())
                .isFree(!course.isPaid())
                .averageRating(course.getAverageRating())
                .reviewCount(course.getReviewCount())
                .enrollmentCount(course.getEnrollmentCount())
                .creatorId(course.getCreator() != null ? course.getCreator().getId() : null)
                .creatorName(course.getCreator() != null ? course.getCreator().getFullName() : null)
                .creatorAvatarUrl(course.getCreator() != null ? course.getCreator().getAvatarUrl() : null)
                .publishedAt(course.getPublishedAt())
                .build();
    }

    private CourseReviewResponse mapToReviewResponse(CourseReview review) {
        return CourseReviewResponse.builder()
                .id(review.getId())
                .courseId(review.getCourse().getId())
                .userId(review.getUser().getId())
                .userFullName(review.getUser().getFullName())
                .userEmail(review.getUser().getEmail())
                .userAvatarUrl(review.getUser().getAvatarUrl())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
