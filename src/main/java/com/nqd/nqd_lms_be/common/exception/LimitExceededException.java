package com.nqd.nqd_lms_be.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class LimitExceededException extends RuntimeException {

    private final String featureKey;
    private final Integer limit;
    private final Integer currentUsage;

    public LimitExceededException(String message) {
        super(message);
        this.featureKey = null;
        this.limit = null;
        this.currentUsage = null;
    }

    public LimitExceededException(String featureKey, Integer limit, Integer currentUsage, String message) {
        super(message);
        this.featureKey = featureKey;
        this.limit = limit;
        this.currentUsage = currentUsage;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getCurrentUsage() {
        return currentUsage;
    }
}
