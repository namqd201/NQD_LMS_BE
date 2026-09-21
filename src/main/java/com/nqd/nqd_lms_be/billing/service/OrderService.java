package com.nqd.nqd_lms_be.billing.service;

import com.nqd.nqd_lms_be.billing.dto.CreateOrderRequest;
import com.nqd.nqd_lms_be.billing.dto.OrderResponse;
import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.entity.Order;
import com.nqd.nqd_lms_be.entity.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    Order createCourseOrder(UUID userId, UUID courseId, String couponCode, String idempotencyKey);

    Order createChapterOrder(UUID userId, UUID chapterId, String couponCode, String idempotencyKey);

    Order createLessonOrder(UUID userId, UUID lessonId, String couponCode, String idempotencyKey);

    Order createMembershipOrder(UUID userId, UUID membershipPlanId, String couponCode, String idempotencyKey);

    Order createOrder(UUID userId, CreateOrderRequest request);

    Order getOrderById(UUID orderId, UUID userId);

    Order getOrderByCode(String orderCode);

    PageResponse<OrderResponse> getMyOrders(UUID userId, Pageable pageable);
    
    PageResponse<OrderResponse> getMyOrders(UUID userId, OrderStatus status, Pageable pageable);

    PageResponse<OrderResponse> getAdminOrders(OrderStatus status, String search, Pageable pageable);

    Order cancelOrder(UUID orderId, UUID userId, String reason);
}
