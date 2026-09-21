package com.nqd.nqd_lms_be.controller.admin;

import com.nqd.nqd_lms_be.billing.dto.OrderResponse;
import com.nqd.nqd_lms_be.billing.service.OrderService;
import com.nqd.nqd_lms_be.common.dto.ApiResponse;
import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.entity.Order;
import com.nqd.nqd_lms_be.entity.enums.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Orders", description = "Administrative endpoints for managing customer orders and purchases")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get all customer orders with status filters and pagination (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "placedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<OrderResponse> response = orderService.getAdminOrders(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách đơn hàng thành công", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details by ID (Admin only)")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID id) {
        Order order = orderService.getOrderById(id, null);
        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết đơn hàng thành công", OrderResponse.fromEntity(order)));
    }
}
