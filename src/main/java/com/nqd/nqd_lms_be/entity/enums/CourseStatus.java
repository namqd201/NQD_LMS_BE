package com.nqd.nqd_lms_be.entity.enums;

public enum CourseStatus {
    DRAFT,
    PENDING_REVIEW,
    PUBLISHED,
    REJECTED,
    ACTIVE,
    ARCHIVED;

    public boolean isPublished() {
        return this == PUBLISHED || this == ACTIVE;
    }
}
