package com.nqd.nqd_lms_be.entity.enums;

public enum SubscriptionStatus {
    PENDING,
    ACTIVE,
    PAST_DUE,
    CANCELED,
    CANCELLED,
    EXPIRED,
    TRIALING;

    public boolean isCancelled() {
        return this == CANCELED || this == CANCELLED;
    }
}
