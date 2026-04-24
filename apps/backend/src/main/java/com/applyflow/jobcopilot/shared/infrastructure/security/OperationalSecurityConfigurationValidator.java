package com.applyflow.jobcopilot.shared.infrastructure.security;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;

@Component
public class OperationalSecurityConfigurationValidator {
    private static final Set<String> STRICT_PROFILES = Set.of("staging", "prod");

    private final Environment environment;
    private final SecurityProperties securityProperties;

    public OperationalSecurityConfigurationValidator(Environment environment, SecurityProperties securityProperties) {
        this.environment = environment;
        this.securityProperties = securityProperties;
    }

    @PostConstruct
    void validate() {
        if (!isStrictProfileActive()) {
            return;
        }

        if (!securityProperties.getRateLimit().isRedisEnabled()) {
            throw new IllegalStateException("security.rate-limit.redis-enabled must be true in staging/prod");
        }

        if (securityProperties.getRateLimit().isFallbackEnabled()) {
            throw new IllegalStateException("security.rate-limit.fallback-enabled must be false in staging/prod");
        }

        String token = securityProperties.getActuator().getMetricsToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("security.actuator.metrics-token is required in staging/prod");
        }
    }

    private boolean isStrictProfileActive() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch(STRICT_PROFILES::contains);
    }
}
