package com.nqd.nqd_lms_be.entity;

import com.nqd.nqd_lms_be.entity.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "coupons",
    indexes = {
        @Index(name = "idx_coupons_code", columnList = "code", unique = true),
        @Index(name = "idx_coupons_active_valid", columnList = "is_active, valid_from, valid_until")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class Coupon extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    public boolean isApplicable(BigDecimal orderAmount) {
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(getIsActive())) return false;
        if (validFrom != null && validFrom.isAfter(now)) return false;
        if (validUntil != null && validUntil.isBefore(now)) return false;
        if (usageLimit != null && usedCount >= usageLimit) return false;
        if (minOrderAmount != null && orderAmount.compareTo(minOrderAmount) < 0) return false;
        return true;
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isApplicable(orderAmount)) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (discountType == DiscountType.PERCENTAGE) {
            BigDecimal factor = discountValue.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            discount = orderAmount.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        } else if (discountType == DiscountType.FIXED_AMOUNT) {
            discount = discountValue.setScale(2, RoundingMode.HALF_UP);
        }

        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }

        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount;
    }
}
