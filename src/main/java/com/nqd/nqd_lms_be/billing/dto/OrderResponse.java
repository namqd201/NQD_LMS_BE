package com.nqd.nqd_lms_be.billing.dto;

import com.nqd.nqd_lms_be.entity.Order;
import com.nqd.nqd_lms_be.entity.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private UUID id;
    private String orderCode;
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String currency;
    private OrderStatus status;
    private String couponCode;
    private String notes;
    private LocalDateTime placedAt;
    private LocalDateTime completedAt;
    private List<OrderItemResponse> items;

    public static OrderResponse fromEntity(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .userEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .userFullName(order.getUser() != null ? order.getUser().getFullName() : null)
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .couponCode(order.getCouponCode())
                .notes(order.getNotes())
                .placedAt(order.getPlacedAt())
                .completedAt(order.getCompletedAt())
                .items(order.getItems() != null ? order.getItems().stream().map(OrderItemResponse::fromEntity).collect(Collectors.toList()) : List.of())
                .build();
    }
}
