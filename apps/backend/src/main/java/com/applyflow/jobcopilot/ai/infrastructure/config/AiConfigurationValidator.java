package com.applyflow.jobcopilot.ai.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AiConfigurationValidator {
    private static final Set<String> STRICT_PROFILES = Set.of("staging", "prod");

    private final Environment environment;
    private final AiProperties properties;

    public AiConfigurationValidator(Environment environment, AiProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @PostConstruct
    void validate() {
        if (!isStrictProfileActive()) {
            return;
        }
        if (!properties.isEnabled()) {
            return;
        }

        AiProperties.Provider provider = properties.getProvider();
        if (provider.getApiKey() == null || provider.getApiKey().isBlank()) {
            throw new IllegalStateException("ai.provider.api-key is required when ai.enabled=true in staging/prod");
        }
        if (provider.getAllowedHosts() == null || provider.getAllowedHosts().isEmpty()) {
            throw new IllegalStateException("ai.provider.allowed-hosts is required when ai.enabled=true in staging/prod");
        }
        URI baseUri = URI.create(provider.getBaseUrl());
        if (!"https".equalsIgnoreCase(baseUri.getScheme())) {
            throw new IllegalStateException("ai.provider.base-url must use https in staging/prod");
        }
        String host = baseUri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("ai.provider.base-url host is invalid");
        }
        Set<String> allowed = provider.getAllowedHosts().stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!allowed.contains(host.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("ai.provider.base-url host must be listed in ai.provider.allowed-hosts");
        }
        if (provider.getConnectTimeoutMs() <= 0 || provider.getReadTimeoutMs() <= 0) {
            throw new IllegalStateException("ai.provider timeout values must be positive");
        }
    }

    private boolean isStrictProfileActive() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch(STRICT_PROFILES::contains);
    }
}

