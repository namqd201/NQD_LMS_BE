package com.nqd.nqd_lms_be.entity.enums;

public enum OrderStatus {
    PENDING,
    PROCESSING,
    PAID,
    COMPLETED,
    CANCELLED,
    REFUNDED,
    EXPIRED;

    public boolean isPaid() {
        return this == PAID || this == COMPLETED;
    }
}
