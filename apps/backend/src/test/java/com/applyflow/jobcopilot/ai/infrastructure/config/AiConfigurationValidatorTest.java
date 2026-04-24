package com.applyflow.jobcopilot.ai.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiConfigurationValidatorTest {

    @Test
    void shouldRejectStagingWhenAiEnabledWithoutApiKey() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("staging");

        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.getProvider().setApiKey("");

        AiConfigurationValidator validator = new AiConfigurationValidator(env, properties);
        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void shouldAllowDevWithoutAiKeyEvenWhenDisabled() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        AiProperties properties = new AiProperties();
        properties.setEnabled(false);

        AiConfigurationValidator validator = new AiConfigurationValidator(env, properties);
        assertDoesNotThrow(validator::validate);
    }
}

