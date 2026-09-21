package com.nqd.nqd_lms_be.service.marketplace;

import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.dto.marketplace.*;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MarketplaceService {
    PageResponse<MarketplaceCourseCardResponse> searchCourses(MarketplaceFilterRequest request, Pageable pageable);
    MarketplaceCourseDetailResponse getCourseDetail(UUID courseId, UUID currentUserId);
    PageResponse<CourseReviewResponse> getCourseReviews(UUID courseId, Pageable pageable);
    CourseReviewResponse createCourseReview(UUID courseId, UUID userId, CreateCourseReviewRequest request);
    CourseReviewResponse updateCourseReview(UUID courseId, UUID reviewId, UUID userId, UpdateCourseReviewRequest request);
    void deleteCourseReview(UUID courseId, UUID reviewId, UUID userId);
}
