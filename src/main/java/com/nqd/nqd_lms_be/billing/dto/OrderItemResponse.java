package com.nqd.nqd_lms_be.billing.dto;

import com.nqd.nqd_lms_be.entity.OrderItem;
import com.nqd.nqd_lms_be.entity.enums.ProductType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private UUID id;
    private UUID productId;
    private String productTitle;
    private ProductType productType;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private String currency;

    public static OrderItemResponse fromEntity(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productTitle(item.getProductTitle())
                .productType(item.getProductType())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .currency(item.getCurrency())
                .build();
    }
}
