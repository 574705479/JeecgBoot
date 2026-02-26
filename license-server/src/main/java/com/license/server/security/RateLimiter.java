package com.license.server.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiter {

    private final Cache<String, AtomicInteger> minuteCounters = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(10000)
            .build();

    private final Cache<String, AtomicInteger> hourCounters = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(10000)
            .build();

    private final Cache<String, Boolean> lockouts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(10000)
            .build();

    public boolean isLockedOut(String key) {
        return lockouts.getIfPresent(key) != null;
    }

    public boolean tryAcquirePerMinute(String key, int limit) {
        AtomicInteger counter = minuteCounters.get(key, k -> new AtomicInteger(0));
        return counter.incrementAndGet() <= limit;
    }

    public boolean tryAcquirePerHour(String key, int limit) {
        AtomicInteger counter = hourCounters.get(key, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();
        if (count > limit) {
            lockouts.put(key, Boolean.TRUE);
            return false;
        }
        return true;
    }
}
