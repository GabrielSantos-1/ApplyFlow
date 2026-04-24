package com.applyflow.jobcopilot.shared.infrastructure.ratelimit;

import com.applyflow.jobcopilot.shared.application.observability.OperationalEventLogger;
import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import com.applyflow.jobcopilot.shared.infrastructure.security.SecurityProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class CompositeRateLimitService implements RateLimitService {
    private final SecurityProperties securityProperties;
    private final RateLimitService inMemoryRateLimitService;
    private final RedisRateLimitService redisRateLimitService;
    private final OperationalMetricsService operationalMetricsService;
    private final OperationalEventLogger operationalEventLogger;

    public CompositeRateLimitService(SecurityProperties securityProperties,
                                     @Qualifier("inMemoryRateLimitService") RateLimitService inMemoryRateLimitService,
                                     RedisRateLimitService redisRateLimitService,
                                     OperationalMetricsService operationalMetricsService,
                                     OperationalEventLogger operationalEventLogger) {
        this.securityProperties = securityProperties;
        this.inMemoryRateLimitService = inMemoryRateLimitService;
        this.redisRateLimitService = redisRateLimitService;
        this.operationalMetricsService = operationalMetricsService;
        this.operationalEventLogger = operationalEventLogger;
    }

    @Override
    public RateLimitDecision evaluate(String key, String policy, int limit, int windowSeconds) {
        if (!securityProperties.getRateLimit().isRedisEnabled()) {
            return inMemoryRateLimitService.evaluate(key, policy, limit, windowSeconds);
        }

        try {
            return redisRateLimitService.evaluate(key, policy, limit, windowSeconds);
        } catch (RateLimitUnavailableException ex) {
            if (securityProperties.getRateLimit().isFallbackEnabled()) {
                operationalMetricsService.recordRateLimitFallback(policy);
                operationalEventLogger.emit("rate_limit_fallback_triggered", "WARN", null, "rate-limit", "fallback", policy);
                return inMemoryRateLimitService.evaluate(key, policy, limit, windowSeconds);
            }
            throw ex;
        }
    }
}
