package com.applyflow.jobcopilot.vacancies.infrastructure.integration;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class IngestionConfigurationValidator {
    private static final Set<String> STRICT_PROFILES = Set.of("staging", "prod");

    private final Environment environment;
    private final IngestionProperties properties;

    public IngestionConfigurationValidator(Environment environment, IngestionProperties properties) {
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
        IngestionProperties.Remotive remotive = properties.getSources().getRemotive();
        if (remotive.getAllowedHosts() == null || remotive.getAllowedHosts().isEmpty()) {
            throw new IllegalStateException("ingestion.sources.remotive.allowed-hosts is required in staging/prod");
        }
        if (remotive.getBaseUrl() == null || !remotive.getBaseUrl().startsWith("https://")) {
            throw new IllegalStateException("ingestion.sources.remotive.base-url must use https in staging/prod");
        }
        if (remotive.getMaxPayloadBytes() <= 0 || remotive.getMaxPayloadBytes() > 5_000_000) {
            throw new IllegalStateException("ingestion.sources.remotive.max-payload-bytes must be within operational bounds");
        }
    }

    private boolean isStrictProfileActive() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch(STRICT_PROFILES::contains);
    }
}
