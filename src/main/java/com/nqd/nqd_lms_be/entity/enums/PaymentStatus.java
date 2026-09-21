package com.nqd.nqd_lms_be.entity.enums;

public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    PAID,
    SUCCESS,
    FAILED,
    CANCELLED,
    REFUNDED,
    EXPIRED;

    public boolean isSuccessful() {
        return this == PAID || this == SUCCESS;
    }
}
