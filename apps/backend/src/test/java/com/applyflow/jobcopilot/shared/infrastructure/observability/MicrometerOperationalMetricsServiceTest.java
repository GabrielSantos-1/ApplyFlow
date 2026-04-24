package com.applyflow.jobcopilot.shared.infrastructure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class MicrometerOperationalMetricsServiceTest {

    @Test
    void metricsEmissionFailureMustNotBreakFlow() {
        MeterRegistry registry = Mockito.mock(MeterRegistry.class);
        Mockito.when(registry.counter(anyString(), any(String[].class)))
                .thenThrow(new IllegalStateException("meter-registry-down"));

        MicrometerOperationalMetricsService service = new MicrometerOperationalMetricsService(registry);

        assertDoesNotThrow(() -> service.recordAuthLogin("success"));
        assertDoesNotThrow(() -> service.recordAuthRefresh("revoked"));
        assertDoesNotThrow(() -> service.recordRateLimitDecision("auth-login", "blocked", "redis"));
        assertDoesNotThrow(() -> service.recordRateLimitFallback("auth-login"));
        assertDoesNotThrow(() -> service.recordAuthorization("forbidden", "api.other"));
        assertDoesNotThrow(() -> service.recordApplicationStatusTransition("DRAFT", "INTERVIEW", "invalid"));
        assertDoesNotThrow(() -> service.recordAuditLogPersistenceFailure("RATE_LIMIT_UNAVAILABLE"));
        assertDoesNotThrow(() -> service.recordAiCallStarted("match-enrichment", "openai-compatible"));
        assertDoesNotThrow(() -> service.recordAiCallCompleted("match-enrichment", "openai-compatible", "success"));
        assertDoesNotThrow(() -> service.recordAiCallDuration("match-enrichment", "openai-compatible", "success", 150));
        assertDoesNotThrow(() -> service.recordAiFallback("match-enrichment", "provider_failed"));
    }
}
