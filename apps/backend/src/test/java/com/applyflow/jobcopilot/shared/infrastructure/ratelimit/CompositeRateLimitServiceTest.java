package com.applyflow.jobcopilot.shared.infrastructure.ratelimit;

import com.applyflow.jobcopilot.shared.application.observability.OperationalEventLogger;
import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import com.applyflow.jobcopilot.shared.infrastructure.security.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompositeRateLimitServiceTest {

    @Test
    void shouldUseFallbackWhenRedisUnavailableAndFallbackEnabled() {
        SecurityProperties props = new SecurityProperties();
        props.getRateLimit().setRedisEnabled(true);
        props.getRateLimit().setFallbackEnabled(true);

        RateLimitService inMemory = Mockito.mock(RateLimitService.class);
        Mockito.when(inMemory.evaluate("key", "policy", 5, 60))
                .thenReturn(new RateLimitDecision(true, 5, 4, 1000, 60, "in-memory-fallback"));

        RedisRateLimitService redis = Mockito.mock(RedisRateLimitService.class);
        Mockito.when(redis.evaluate("key", "policy", 5, 60))
                .thenThrow(new RateLimitUnavailableException("redis down"));
        OperationalMetricsService metrics = Mockito.mock(OperationalMetricsService.class);
        OperationalEventLogger events = Mockito.mock(OperationalEventLogger.class);

        CompositeRateLimitService service = new CompositeRateLimitService(props, inMemory, redis, metrics, events);
        RateLimitDecision decision = service.evaluate("key", "policy", 5, 60);

        assertEquals("in-memory-fallback", decision.mode());
    }

    @Test
    void shouldFailWhenRedisUnavailableAndFallbackDisabled() {
        SecurityProperties props = new SecurityProperties();
        props.getRateLimit().setRedisEnabled(true);
        props.getRateLimit().setFallbackEnabled(false);

        RateLimitService inMemory = Mockito.mock(RateLimitService.class);
        RedisRateLimitService redis = Mockito.mock(RedisRateLimitService.class);
        Mockito.when(redis.evaluate("key", "policy", 5, 60))
                .thenThrow(new RateLimitUnavailableException("redis down"));
        OperationalMetricsService metrics = Mockito.mock(OperationalMetricsService.class);
        OperationalEventLogger events = Mockito.mock(OperationalEventLogger.class);

        CompositeRateLimitService service = new CompositeRateLimitService(props, inMemory, redis, metrics, events);
        assertThrows(RateLimitUnavailableException.class, () -> service.evaluate("key", "policy", 5, 60));
    }
}
