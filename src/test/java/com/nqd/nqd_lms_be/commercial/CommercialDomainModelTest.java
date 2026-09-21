package com.nqd.nqd_lms_be.commercial;

import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CommercialDomainModelTest {

    @Test
    @DisplayName("Should build Product with valid defaults and optimistic locking version")
    void testProductCreation() {
        Product product = Product.builder()
                .code("PRD-MATH-12")
                .title("Khóa luyện thi Toán 12 Nâng Cao")
                .productType(ProductType.COURSE)
                .targetEntityId(UUID.randomUUID())
                .basePrice(new BigDecimal("499000.00"))
                .currency("VND")
                .status(ProductStatus.PUBLISHED)
                .build();

        assertEquals("PRD-MATH-12", product.getCode());
        assertEquals(ProductType.COURSE, product.getProductType());
        assertEquals(new BigDecimal("499000.00"), product.getBasePrice());
        assertEquals(0L, product.getVersion());
    }

    @Test
    @DisplayName("Should build Order and calculate correct final amount")
    void testOrderCreationAndCalculations() {
        User student = User.builder().email("student@test.com").fullName("Nguyễn Văn A").build();

        Order order = Order.builder()
                .orderCode("ORD-2026-TEST01")
                .user(student)
                .totalAmount(new BigDecimal("1000000.00"))
                .discountAmount(new BigDecimal("200000.00"))
                .finalAmount(new BigDecimal("800000.00"))
                .currency("VND")
                .status(OrderStatus.PENDING)
                .idempotencyKey("IDEMP-KEY-999")
                .build();

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals("IDEMP-KEY-999", order.getIdempotencyKey());
        assertEquals(new BigDecimal("800000.00"), order.getFinalAmount());
    }

    @Test
    @DisplayName("Coupon should accurately validate applicability and compute discount")
    void testCouponDiscountCalculation() {
        Coupon percentageCoupon = Coupon.builder()
                .code("SALE20")
                .name("Giảm giá 20%")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .minOrderAmount(new BigDecimal("200000.00"))
                .maxDiscountAmount(new BigDecimal("100000.00"))
                .usageLimit(100)
                .usedCount(5)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(10))
                .build();
        percentageCoupon.setIsActive(true);

        // Case 1: Order 300,000 -> 20% is 60,000 (under max 100,000)
        assertTrue(percentageCoupon.isApplicable(new BigDecimal("300000.00")));
        BigDecimal discount1 = percentageCoupon.calculateDiscount(new BigDecimal("300000.00"));
        assertEquals(new BigDecimal("60000.00"), discount1);

        // Case 2: Order 1,000,000 -> 20% is 200,000 -> Capped at maxDiscountAmount 100,000
        BigDecimal discount2 = percentageCoupon.calculateDiscount(new BigDecimal("1000000.00"));
        assertEquals(new BigDecimal("100000.00"), discount2);

        // Case 3: Order below minimum 200,000 (150,000) -> 0 discount
        assertFalse(percentageCoupon.isApplicable(new BigDecimal("150000.00")));
        BigDecimal discount3 = percentageCoupon.calculateDiscount(new BigDecimal("150000.00"));
        assertEquals(BigDecimal.ZERO, discount3);
    }

    @Test
    @DisplayName("Entitlement and Subscription validity check")
    void testEntitlementAndSubscriptionValidity() {
        User user = User.builder().email("student@test.com").fullName("Test User").build();
        MembershipPlan plan = MembershipPlan.builder().planCode("PRO_MONTHLY").name("Pro Monthly").price(new BigDecimal("99000.00")).build();

        Subscription activeSub = Subscription.builder()
                .user(user)
                .membershipPlan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now().minusDays(5))
                .endDate(LocalDateTime.now().plusDays(25))
                .build();

        assertTrue(activeSub.isCurrentlyActive());

        Subscription expiredSub = Subscription.builder()
                .user(user)
                .membershipPlan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now().minusDays(40))
                .endDate(LocalDateTime.now().minusDays(10))
                .build();

        assertFalse(expiredSub.isCurrentlyActive());

        Entitlement activeEntitlement = Entitlement.builder()
                .user(user)
                .entitlementType(EntitlementType.COURSE_ACCESS)
                .targetEntityId(UUID.randomUUID())
                .status(EntitlementStatus.ACTIVE)
                .validFrom(LocalDateTime.now().minusHours(1))
                .validUntil(LocalDateTime.now().plusDays(365))
                .build();

        assertTrue(activeEntitlement.isCurrentlyValid());
    }
}
