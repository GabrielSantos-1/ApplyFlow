package com.applyflow.jobcopilot.shared.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperationalSecurityConfigurationValidatorTest {

    @Test
    void shouldRejectStagingWhenMetricsTokenMissing() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");

        SecurityProperties properties = new SecurityProperties();
        properties.getRateLimit().setRedisEnabled(true);
        properties.getRateLimit().setFallbackEnabled(false);
        properties.getActuator().setMetricsToken("");

        OperationalSecurityConfigurationValidator validator =
                new OperationalSecurityConfigurationValidator(environment, properties);

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void shouldRejectStagingWhenFallbackIsEnabled() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");

        SecurityProperties properties = new SecurityProperties();
        properties.getRateLimit().setRedisEnabled(true);
        properties.getRateLimit().setFallbackEnabled(true);
        properties.getActuator().setMetricsToken("metrics-token");

        OperationalSecurityConfigurationValidator validator =
                new OperationalSecurityConfigurationValidator(environment, properties);

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void shouldAllowDevProfileWithoutStrictOperationalRequirements() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        SecurityProperties properties = new SecurityProperties();
        properties.getRateLimit().setRedisEnabled(false);
        properties.getRateLimit().setFallbackEnabled(true);
        properties.getActuator().setMetricsToken(null);

        OperationalSecurityConfigurationValidator validator =
                new OperationalSecurityConfigurationValidator(environment, properties);

        assertDoesNotThrow(validator::validate);
    }
}
