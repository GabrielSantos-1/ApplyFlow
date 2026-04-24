package com.applyflow.jobcopilot.shared.infrastructure.ratelimit;

import com.applyflow.jobcopilot.shared.infrastructure.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class RedisRateLimitService implements RateLimitService {
    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitService.class);

    private static final DefaultRedisScript<List> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]) " +
                    "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
                    "local ttl = redis.call('TTL', KEYS[1]) " +
                    "if ttl < 0 then ttl = ARGV[1] end " +
                    "return {current, ttl}",
            List.class
    );

    private final StringRedisTemplate redisTemplate;
    private final SecurityProperties securityProperties;

    public RedisRateLimitService(StringRedisTemplate redisTemplate, SecurityProperties securityProperties) {
        this.redisTemplate = redisTemplate;
        this.securityProperties = securityProperties;
    }

    @Override
    public RateLimitDecision evaluate(String key, String policy, int limit, int windowSeconds) {
        String redisKey = securityProperties.getRateLimit().getRedisKeyPrefix() + policy + ":" + key;
        long now = Instant.now().getEpochSecond();

        try {
            applySimulation(policy);
            List result = redisTemplate.execute(RATE_LIMIT_SCRIPT, List.of(redisKey), String.valueOf(windowSeconds));
            if (result == null || result.size() < 2) {
                throw new RateLimitUnavailableException("Redis rate limit script returned invalid payload");
            }

            long count = ((Number) result.get(0)).longValue();
            long ttl = ((Number) result.get(1)).longValue();
            long resetAt = now + Math.max(0, ttl);

            boolean allowed = count <= limit;
            int remaining = Math.max(0, (int) (limit - count));
            int retryAfter = (int) Math.max(0, ttl);
            return new RateLimitDecision(allowed, limit, remaining, resetAt, retryAfter, "redis");
        } catch (Exception ex) {
            log.warn("rate_limit.redis_unavailable policy={} keyHash={} reason={}", policy, Integer.toHexString(key.hashCode()), ex.toString());
            throw new RateLimitUnavailableException("Redis indisponivel para rate limit", ex);
        }
    }

    private void applySimulation(String policy) {
        SecurityProperties.RateLimit cfg = securityProperties.getRateLimit();
        if (!cfg.isSimulationEnabled()) {
            return;
        }
        String mode = cfg.getSimulationMode() == null ? "none" : cfg.getSimulationMode().trim().toLowerCase();
        switch (mode) {
            case "unavailable" -> throw new RateLimitUnavailableException("Redis indisponivel (simulacao)");
            case "latency" -> {
                long ms = Math.max(0, cfg.getSimulatedLatencyMs());
                if (ms > 0) {
                    try {
                        Thread.sleep(ms);
                        log.warn("eventType=rate_limit_simulated_latency severity=WARN policy={} delayMs={}", policy, ms);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new RateLimitUnavailableException("Interrupcao durante simulacao de latencia", ex);
                    }
                }
            }
            default -> {
                // no simulation
            }
        }
    }
}
