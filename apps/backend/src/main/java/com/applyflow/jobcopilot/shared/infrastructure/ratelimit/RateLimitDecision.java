package com.applyflow.jobcopilot.shared.infrastructure.ratelimit;

public record RateLimitDecision(boolean allowed,
                                int limit,
                                int remaining,
                                long resetEpochSeconds,
                                int retryAfterSeconds,
                                String mode) {
}
