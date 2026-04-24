package com.applyflow.jobcopilot.shared.infrastructure.ratelimit;

public class RateLimitUnavailableException extends RuntimeException {
    public RateLimitUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public RateLimitUnavailableException(String message) {
        super(message);
    }
}
