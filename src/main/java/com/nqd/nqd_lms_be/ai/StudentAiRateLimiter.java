package com.nqd.nqd_lms_be.ai;

import com.nqd.nqd_lms_be.common.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class StudentAiRateLimiter {

    private final int maxRequestsPerMinute;
    private final Map<UUID, Deque<Long>> studentRequestTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, ReentrantLock> studentLocks = new ConcurrentHashMap<>();

    public StudentAiRateLimiter(
            @Value("${lms.ai.rate-limit.student-per-minute:20}") int maxRequestsPerMinute
    ) {
        this.maxRequestsPerMinute = Math.max(1, maxRequestsPerMinute);
    }

    public void checkAndAcquire(UUID studentId) {
        if (studentId == null) {
            return;
        }

        ReentrantLock lock = studentLocks.computeIfAbsent(studentId, k -> new ReentrantLock());
        lock.lock();
        try {
            long now = Instant.now().toEpochMilli();
            long windowStart = now - 60_000L; // 1 minute window

            Deque<Long> timestamps = studentRequestTimestamps.computeIfAbsent(studentId, k -> new ArrayDeque<>());

            // Evict timestamps outside 60s window
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequestsPerMinute) {
                log.warn("Student {} exceeded AI Tutor rate limit ({} requests/min)", studentId, maxRequestsPerMinute);
                throw new RateLimitExceededException("Bạn đã vượt quá giới hạn yêu cầu AI Tutor (" + maxRequestsPerMinute + " lượt/phút). Vui lòng đợi trong giây lát!");
            }

            timestamps.addLast(now);
        } finally {
            lock.unlock();
        }
    }

    public int getRemainingRequests(UUID studentId) {
        if (studentId == null) {
            return maxRequestsPerMinute;
        }

        ReentrantLock lock = studentLocks.computeIfAbsent(studentId, k -> new ReentrantLock());
        lock.lock();
        try {
            long now = Instant.now().toEpochMilli();
            long windowStart = now - 60_000L;

            Deque<Long> timestamps = studentRequestTimestamps.get(studentId);
            if (timestamps == null) {
                return maxRequestsPerMinute;
            }

            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            return Math.max(0, maxRequestsPerMinute - timestamps.size());
        } finally {
            lock.unlock();
        }
    }

    public void resetForStudent(UUID studentId) {
        if (studentId != null) {
            studentRequestTimestamps.remove(studentId);
            studentLocks.remove(studentId);
        }
    }
}
