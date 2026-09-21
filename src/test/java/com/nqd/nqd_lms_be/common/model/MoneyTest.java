package com.nqd.nqd_lms_be.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    @DisplayName("Should create Money with default VND and scale 2")
    void testCreateMoneyDefaultVND() {
        Money money = Money.of(150000);
        assertEquals(new BigDecimal("150000.00"), money.getAmount());
        assertEquals("VND", money.getCurrency());
        assertTrue(money.isPositive());
        assertFalse(money.isZero());
        assertFalse(money.isNegative());
    }

    @Test
    @DisplayName("Should add two Money instances of same currency")
    void testMoneyAddition() {
        Money m1 = Money.of(new BigDecimal("100.50"), "VND");
        Money m2 = Money.of(new BigDecimal("200.25"), "VND");
        Money sum = m1.add(m2);

        assertEquals(new BigDecimal("300.75"), sum.getAmount());
        assertEquals("VND", sum.getCurrency());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when adding different currencies")
    void testMoneyAdditionCurrencyMismatch() {
        Money vnd = Money.of(100, "VND");
        Money usd = Money.of(5, "USD");

        assertThrows(IllegalArgumentException.class, () -> vnd.add(usd));
    }

    @Test
    @DisplayName("Should calculate percentage with HALF_UP rounding")
    void testMoneyPercentage() {
        Money original = Money.of(new BigDecimal("1000000.00"), "VND");
        Money discount = original.percentage(new BigDecimal("15.5")); // 15.5%

        assertEquals(new BigDecimal("155000.00"), discount.getAmount());
    }

    @Test
    @DisplayName("Should correctly compare Money instances")
    void testMoneyComparisons() {
        Money m1 = Money.of(50000);
        Money m2 = Money.of(100000);

        assertTrue(m1.isLessThan(m2));
        assertTrue(m2.isGreaterThan(m1));
        assertTrue(m1.isLessThanOrEqual(m2));
        assertTrue(m2.isGreaterThanOrEqual(m1));
        assertNotEquals(m1, m2);
    }
}
