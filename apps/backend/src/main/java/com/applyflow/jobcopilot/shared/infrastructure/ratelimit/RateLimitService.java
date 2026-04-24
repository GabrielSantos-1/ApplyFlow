package com.applyflow.jobcopilot.shared.infrastructure.ratelimit;

public interface RateLimitService {
    RateLimitDecision evaluate(String key, String policy, int limit, int windowSeconds);
}
