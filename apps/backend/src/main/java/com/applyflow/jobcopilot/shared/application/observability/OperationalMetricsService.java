package com.applyflow.jobcopilot.shared.application.observability;

public interface OperationalMetricsService {
    void recordAuthLogin(String outcome);

    void recordAuthRefresh(String outcome);

    void recordRateLimitDecision(String policy, String outcome, String mode);

    void recordRateLimitFallback(String policy);

    void recordAuthorization(String outcome, String endpoint);

    void recordApplicationStatusTransition(String from, String to, String outcome);

    void recordAuditLogPersistenceFailure(String action);

    void recordAiCallStarted(String flow, String provider);

    void recordAiCallCompleted(String flow, String provider, String outcome);

    void recordAiCallDuration(String flow, String provider, String outcome, long durationMs);

    void recordAiFallback(String flow, String reason);

    void recordResumeUpload(String stage, String outcome);

    void recordResumeUploadValidationFailure(String reason);

    void recordMatchGenerated(String outcome, String recommendation);

    void recordMatchGenerationDuration(String outcome, long durationMs);

    void recordMatchContextMissing(String reason);
}
