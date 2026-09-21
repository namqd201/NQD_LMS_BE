package com.nqd.nqd_lms_be.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable Value Object representing Monetary Values in NQD-LMS Commercial Domain.
 * Strictly enforces BigDecimal, Currency validation, and RoundingMode.HALF_UP.
 */
@Getter
@EqualsAndHashCode
public final class Money implements Serializable, Comparable<Money> {

    public static final String DEFAULT_CURRENCY = "VND";
    public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final int DEFAULT_DECIMAL_SCALE = 2;

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "Money amount cannot be null");
        Objects.requireNonNull(currency, "Currency code cannot be null");

        String normalizedCurrency = currency.trim().toUpperCase();
        validateCurrency(normalizedCurrency);

        this.currency = normalizedCurrency;
        // Normalize scale with HALF_UP rounding
        this.amount = amount.setScale(DEFAULT_DECIMAL_SCALE, DEFAULT_ROUNDING_MODE);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }

    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount), DEFAULT_CURRENCY);
    }

    public static Money of(long amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money ofString(String amountStr, String currency) {
        Objects.requireNonNull(amountStr, "Amount string cannot be null");
        return new Money(new BigDecimal(amountStr.trim()), currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO, DEFAULT_CURRENCY);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        return new Money(result, this.currency);
    }

    public Money multiply(long factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "Multiplication factor cannot be null");
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public Money percentage(BigDecimal percent) {
        Objects.requireNonNull(percent, "Percentage cannot be null");
        BigDecimal factor = percent.divide(BigDecimal.valueOf(100), 4, DEFAULT_ROUNDING_MODE);
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return compareTo(other) >= 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        return compareTo(other) <= 0;
    }

    private void assertSameCurrency(Money other) {
        Objects.requireNonNull(other, "Money operand cannot be null");
        if (!this.currency.equalsIgnoreCase(other.currency)) {
            throw new IllegalArgumentException(String.format("Currency mismatch: Cannot operate between %s and %s", this.currency, other.currency));
        }
    }

    private static void validateCurrency(String currencyCode) {
        try {
            Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            // Allow standard ISO codes or internal custom codes
            if (!currencyCode.matches("^[A-Z]{3}$")) {
                throw new IllegalArgumentException("Invalid currency code: " + currencyCode);
            }
        }
    }

    @Override
    public int compareTo(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
