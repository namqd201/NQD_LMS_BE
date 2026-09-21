package com.nqd.nqd_lms_be.discussion;

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
public class DiscussionRateLimiter {

    private final int maxPostsPerMinute;
    private final Map<UUID, Deque<Long>> userTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    public DiscussionRateLimiter(
            @Value("${lms.discussion.rate-limit.posts-per-minute:10}") int maxPostsPerMinute
    ) {
        this.maxPostsPerMinute = Math.max(1, maxPostsPerMinute);
    }

    public void checkAndAcquire(UUID userId) {
        if (userId == null) return;

        ReentrantLock lock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());
        lock.lock();
        try {
            long now = Instant.now().toEpochMilli();
            long windowStart = now - 60_000L;

            Deque<Long> timestamps = userTimestamps.computeIfAbsent(userId, k -> new ArrayDeque<>());
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxPostsPerMinute) {
                log.warn("User {} exceeded discussion rate limit ({} requests/min)", userId, maxPostsPerMinute);
                throw new RateLimitExceededException("Bạn đang đăng thảo luận quá nhanh (" + maxPostsPerMinute + " lượt/phút). Vui lòng đợi trong giây lát!");
            }

            timestamps.addLast(now);
        } finally {
            lock.unlock();
        }
    }
}
