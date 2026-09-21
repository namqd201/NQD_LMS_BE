package com.nqd.nqd_lms_be.billing.dto;

import com.nqd.nqd_lms_be.entity.enums.ProductType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    private UUID courseId;

    private UUID chapterId;

    private UUID lessonId;

    private UUID membershipPlanId;

    private ProductType productType;

    private String couponCode;

    private String notes;

    private String idempotencyKey;

    private List<OrderItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemRequest {
        @NotNull(message = "Product ID không được để trống")
        private UUID productId;

        @Builder.Default
        private Integer quantity = 1;
    }
}
