package com.nqd.nqd_lms_be.controller.billing;

import com.nqd.nqd_lms_be.billing.dto.CreateOrderRequest;
import com.nqd.nqd_lms_be.billing.dto.OrderResponse;
import com.nqd.nqd_lms_be.billing.service.OrderService;
import com.nqd.nqd_lms_be.common.dto.ApiResponse;
import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.entity.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Billing - Orders", description = "Endpoints for managing orders, checkout intents and purchases")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Create an order to purchase a course")
    public ResponseEntity<ApiResponse<OrderResponse>> createCourseOrder(
            @PathVariable UUID courseId,
            @RequestParam(required = false) String couponCode,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        Order order = orderService.createCourseOrder(principal.getId(), courseId, couponCode, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo đơn hàng mua khóa học thành công", OrderResponse.fromEntity(order)));
    }

    @PostMapping("/chapter/{chapterId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Create an order to purchase a chapter")
    public ResponseEntity<ApiResponse<OrderResponse>> createChapterOrder(
            @PathVariable UUID chapterId,
            @RequestParam(required = false) String couponCode,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        Order order = orderService.createChapterOrder(principal.getId(), chapterId, couponCode, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo đơn hàng mua chương học thành công", OrderResponse.fromEntity(order)));
    }

    @PostMapping("/lesson/{lessonId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Create an order to purchase a lesson")
    public ResponseEntity<ApiResponse<OrderResponse>> createLessonOrder(
            @PathVariable UUID lessonId,
            @RequestParam(required = false) String couponCode,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        Order order = orderService.createLessonOrder(principal.getId(), lessonId, couponCode, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo đơn hàng mua bài học thành công", OrderResponse.fromEntity(order)));
    }

    @PostMapping("/membership/{planId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Create an order to subscribe to a membership plan")
    public ResponseEntity<ApiResponse<OrderResponse>> createMembershipOrder(
            @PathVariable UUID planId,
            @RequestParam(required = false) String couponCode,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        Order order = orderService.createMembershipOrder(principal.getId(), planId, couponCode, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo đơn hàng đăng ký gói hội viên thành công", OrderResponse.fromEntity(order)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Create generic order with custom line items")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        Order order = orderService.createOrder(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo đơn hàng thành công", OrderResponse.fromEntity(order)));
    }

    @GetMapping({"/me", "/my-orders"})
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Get current authenticated user's orders history")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @RequestParam(required = false) com.nqd.nqd_lms_be.entity.enums.OrderStatus status,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PageableDefault(size = 10, sort = "placedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<OrderResponse> response = orderService.getMyOrders(principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách đơn hàng thành công", response));
    }

    @GetMapping("/code/{orderCode}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Get order details by order code")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByCode(
            @PathVariable String orderCode,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        Order order = orderService.getOrderByCode(orderCode);
        if (principal != null && !order.getUser().getId().equals(principal.getId())) {
            throw new ForbiddenOperationException("Bạn không có quyền xem đơn hàng của người dùng khác.");
        }
        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết đơn hàng thành công", OrderResponse.fromEntity(order)));
    }

    @GetMapping("/{identifier}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Get order details by ID or Order Code")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByIdentifier(
            @PathVariable String identifier,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        Order order;
        try {
            UUID id = UUID.fromString(identifier);
            order = orderService.getOrderById(id, principal.getId());
        } catch (IllegalArgumentException e) {
            order = orderService.getOrderByCode(identifier);
            if (principal != null && !order.getUser().getId().equals(principal.getId())) {
                throw new ForbiddenOperationException("Bạn không có quyền xem đơn hàng của người dùng khác.");
            }
        }
        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết đơn hàng thành công", OrderResponse.fromEntity(order)));
    }

    @PutMapping("/{identifier}/cancel")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Cancel a pending order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable String identifier,
            @RequestParam(defaultValue = "Khách hàng tự hủy đơn") String reason,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        UUID orderId;
        try {
            orderId = UUID.fromString(identifier);
        } catch (IllegalArgumentException e) {
            Order order = orderService.getOrderByCode(identifier);
            orderId = order.getId();
        }
        Order order = orderService.cancelOrder(orderId, principal.getId(), reason);
        return ResponseEntity.ok(ApiResponse.ok("Hủy đơn hàng thành công", OrderResponse.fromEntity(order)));
    }
}
