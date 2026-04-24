package com.applyflow.jobcopilot.shared.infrastructure.ratelimit;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("inMemoryRateLimitService")
public class InMemoryRateLimitService implements RateLimitService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public RateLimitDecision evaluate(String key, String policy, int limit, int windowSeconds) {
        String composed = policy + ":" + key;
        long now = Instant.now().getEpochSecond();
        Bucket bucket = buckets.compute(composed, (k, existing) -> {
            if (existing == null || now >= existing.resetAtEpochSeconds()) {
                return new Bucket(1, now + windowSeconds);
            }
            return new Bucket(existing.count() + 1, existing.resetAtEpochSeconds());
        });

        boolean allowed = bucket.count() <= limit;
        int remaining = Math.max(0, limit - bucket.count());
        int retryAfter = (int) Math.max(0, bucket.resetAtEpochSeconds() - now);
        return new RateLimitDecision(allowed, limit, remaining, bucket.resetAtEpochSeconds(), retryAfter, "in-memory-fallback");
    }

    private record Bucket(int count, long resetAtEpochSeconds) {
    }
}
