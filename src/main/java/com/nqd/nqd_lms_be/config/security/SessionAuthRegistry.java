package com.nqd.nqd_lms_be.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionAuthRegistry {

    private final Map<String, Authentication> authStore = new ConcurrentHashMap<>();

    public void register(String sessionId, Authentication authentication) {
        if (sessionId != null && authentication != null) {
            authStore.put(sessionId, authentication);
        }
    }

    public Authentication get(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        return authStore.get(sessionId);
    }

    public void remove(String sessionId) {
        if (sessionId != null) {
            authStore.remove(sessionId);
        }
    }
}
