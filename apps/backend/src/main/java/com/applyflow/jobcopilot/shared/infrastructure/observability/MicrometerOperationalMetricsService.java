package com.applyflow.jobcopilot.shared.infrastructure.observability;

import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MicrometerOperationalMetricsService implements OperationalMetricsService {
    private static final Logger log = LoggerFactory.getLogger(MicrometerOperationalMetricsService.class);

    private final MeterRegistry meterRegistry;

    public MicrometerOperationalMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordAuthLogin(String outcome) {
        safeRecord("auth.login", () -> meterRegistry.counter("applyflow_auth_login_total",
                "outcome", sanitizeTag(outcome)).increment());
    }

    @Override
    public void recordAuthRefresh(String outcome) {
        safeRecord("auth.refresh", () -> meterRegistry.counter("applyflow_auth_refresh_total",
                "outcome", sanitizeTag(outcome)).increment());
    }

    @Override
    public void recordRateLimitDecision(String policy, String outcome, String mode) {
        safeRecord("rate_limit.decision", () -> meterRegistry.counter("applyflow_rate_limit_total",
                "policy", sanitizeTag(policy),
                "outcome", sanitizeTag(outcome),
                "mode", sanitizeTag(mode)).increment());
    }

    @Override
    public void recordRateLimitFallback(String policy) {
        safeRecord("rate_limit.fallback", () -> meterRegistry.counter("applyflow_rate_limit_fallback_total",
                "policy", sanitizeTag(policy)).increment());
    }

    @Override
    public void recordAuthorization(String outcome, String endpoint) {
        safeRecord("security.authorization", () -> meterRegistry.counter("applyflow_authorization_total",
                "outcome", sanitizeTag(outcome),
                "endpoint", sanitizeTag(endpoint)).increment());
    }

    @Override
    public void recordApplicationStatusTransition(String from, String to, String outcome) {
        safeRecord("applications.status_transition", () -> meterRegistry.counter("applyflow_application_status_transition_total",
                "from", sanitizeTag(from),
                "to", sanitizeTag(to),
                "outcome", sanitizeTag(outcome)).increment());
    }

    @Override
    public void recordAuditLogPersistenceFailure(String action) {
        safeRecord("audit.persist_failure", () -> meterRegistry.counter("applyflow_audit_log_persist_failures_total",
                "action", sanitizeTag(action)).increment());
    }

    @Override
    public void recordAiCallStarted(String flow, String provider) {
        safeRecord("ai.started", () -> meterRegistry.counter("applyflow_ai_calls_started_total",
                "flow", sanitizeTag(flow),
                "provider", sanitizeTag(provider)).increment());
    }

    @Override
    public void recordAiCallCompleted(String flow, String provider, String outcome) {
        safeRecord("ai.completed", () -> meterRegistry.counter("applyflow_ai_calls_completed_total",
                "flow", sanitizeTag(flow),
                "provider", sanitizeTag(provider),
                "outcome", sanitizeTag(outcome)).increment());
    }

    @Override
    public void recordAiCallDuration(String flow, String provider, String outcome, long durationMs) {
        safeRecord("ai.duration", () -> meterRegistry.timer("applyflow_ai_call_duration",
                "flow", sanitizeTag(flow),
                "provider", sanitizeTag(provider),
                "outcome", sanitizeTag(outcome)).record(java.time.Duration.ofMillis(Math.max(1L, durationMs))));
    }

    @Override
    public void recordAiFallback(String flow, String reason) {
        safeRecord("ai.fallback", () -> meterRegistry.counter("applyflow_ai_fallback_total",
                "flow", sanitizeTag(flow),
                "reason", sanitizeTag(reason)).increment());
    }

    @Override
    public void recordResumeUpload(String stage, String outcome) {
        safeRecord("resume.upload", () -> meterRegistry.counter("applyflow_resume_upload_total",
                "stage", sanitizeTag(stage),
                "outcome", sanitizeTag(outcome)).increment());
    }

    @Override
    public void recordResumeUploadValidationFailure(String reason) {
        safeRecord("resume.upload_validation", () -> meterRegistry.counter("applyflow_resume_upload_validation_failures_total",
                "reason", sanitizeTag(reason)).increment());
    }

    @Override
    public void recordMatchGenerated(String outcome, String recommendation) {
        safeRecord("matching.generated", () -> meterRegistry.counter("applyflow_matches_generated_total",
                "outcome", sanitizeTag(outcome),
                "recommendation", sanitizeTag(recommendation)).increment());
    }

    @Override
    public void recordMatchGenerationDuration(String outcome, long durationMs) {
        safeRecord("matching.duration", () -> meterRegistry.timer("applyflow_match_generation_duration",
                "outcome", sanitizeTag(outcome))
                .record(java.time.Duration.ofMillis(Math.max(1L, durationMs))));
    }

    @Override
    public void recordMatchContextMissing(String reason) {
        safeRecord("matching.context_missing", () -> meterRegistry.counter("applyflow_match_context_missing_total",
                "reason", sanitizeTag(reason)).increment());
    }

    private void safeRecord(String signal, Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            log.warn("eventType=observability.metric_emission_failed severity=WARN signal={} reason={}", signal, ex.toString());
            try {
                meterRegistry.counter("applyflow_observability_emit_failures_total", "signal", sanitizeTag(signal)).increment();
            } catch (Exception ignored) {
                // Explicit no-op: observability failures cannot break request flow.
            }
        }
    }

    private String sanitizeTag(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String sanitized = value.trim().toLowerCase().replaceAll("[^a-z0-9_.:-]", "_");
        return sanitized.length() > 60 ? sanitized.substring(0, 60) : sanitized;
    }
}
