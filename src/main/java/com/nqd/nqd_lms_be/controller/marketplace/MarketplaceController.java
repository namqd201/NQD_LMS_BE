package com.nqd.nqd_lms_be.controller.marketplace;

import com.nqd.nqd_lms_be.common.dto.ApiResponse;
import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.dto.marketplace.*;
import com.nqd.nqd_lms_be.service.marketplace.MarketplaceService;
import com.nqd.nqd_lms_be.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/marketplace")
@RequiredArgsConstructor
@Tag(name = "Marketplace", description = "Marketplace endpoints for searching, previewing, and reviewing courses")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @GetMapping("/courses")
    @Operation(summary = "Search and filter courses on marketplace with pagination")
    public ResponseEntity<ApiResponse<PageResponse<MarketplaceCourseCardResponse>>> searchCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) String gradeLevel,
            @RequestParam(required = false) UUID teacherId,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false, defaultValue = "newest") String sortBy,
            @PageableDefault(size = 12) Pageable pageable) {

        MarketplaceFilterRequest request = MarketplaceFilterRequest.builder()
                .keyword(keyword)
                .subjectId(subjectId)
                .gradeLevel(gradeLevel)
                .teacherId(teacherId)
                .isFree(isFree)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minRating(minRating)
                .sortBy(sortBy)
                .build();

        PageResponse<MarketplaceCourseCardResponse> result = marketplaceService.searchCourses(request, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/courses/{id}")
    @Operation(summary = "Get detailed marketplace course view, preview curriculum, and enrollment/purchase status")
    public ResponseEntity<ApiResponse<MarketplaceCourseDetailResponse>> getCourseDetail(@PathVariable UUID id) {
        UUID currentUserId = null;
        try {
            currentUserId = SecurityUtils.getCurrentUserId();
        } catch (Exception ignored) {
            // Unauthenticated guest user
        }

        MarketplaceCourseDetailResponse result = marketplaceService.getCourseDetail(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/courses/{id}/reviews")
    @Operation(summary = "Get paginated reviews for a course")
    public ResponseEntity<ApiResponse<PageResponse<CourseReviewResponse>>> getCourseReviews(
            @PathVariable UUID id,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<CourseReviewResponse> result = marketplaceService.getCourseReviews(id, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/courses/{id}/reviews")
    @Operation(summary = "Submit a review for a purchased or enrolled course (1 review per student)")
    public ResponseEntity<ApiResponse<CourseReviewResponse>> createCourseReview(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCourseReviewRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        CourseReviewResponse result = marketplaceService.createCourseReview(id, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Đánh giá khóa học thành công", result));
    }

    @PutMapping("/courses/{id}/reviews/{reviewId}")
    @Operation(summary = "Update an existing review")
    public ResponseEntity<ApiResponse<CourseReviewResponse>> updateCourseReview(
            @PathVariable UUID id,
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateCourseReviewRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        CourseReviewResponse result = marketplaceService.updateCourseReview(id, reviewId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật đánh giá thành công", result));
    }

    @DeleteMapping("/courses/{id}/reviews/{reviewId}")
    @Operation(summary = "Delete an existing review")
    public ResponseEntity<ApiResponse<Void>> deleteCourseReview(
            @PathVariable UUID id,
            @PathVariable UUID reviewId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        marketplaceService.deleteCourseReview(id, reviewId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Xóa đánh giá thành công", null));
    }
}
