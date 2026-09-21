package com.nqd.nqd_lms_be.entity.enums;

public enum ProductType {
    COURSE,
    COURSE_PURCHASE,
    EXAM_PACKAGE,
    MEMBERSHIP,
    MEMBERSHIP_PURCHASE,
    AI_CREDIT_PACK,
    CHAPTER_PURCHASE,
    LESSON_PURCHASE;

    public boolean isCourse() {
        return this == COURSE || this == COURSE_PURCHASE;
    }

    public boolean isChapter() {
        return this == CHAPTER_PURCHASE;
    }

    public boolean isLesson() {
        return this == LESSON_PURCHASE;
    }

    public boolean isMembership() {
        return this == MEMBERSHIP || this == MEMBERSHIP_PURCHASE;
    }

    public boolean isEducationalContent() {
        return isCourse() || isChapter() || isLesson() || this == EXAM_PACKAGE;
    }
}
