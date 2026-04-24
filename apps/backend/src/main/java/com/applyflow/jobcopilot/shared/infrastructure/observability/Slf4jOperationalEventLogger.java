package com.applyflow.jobcopilot.shared.infrastructure.observability;

import com.applyflow.jobcopilot.shared.application.observability.OperationalEventLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class Slf4jOperationalEventLogger implements OperationalEventLogger {
    private static final Logger log = LoggerFactory.getLogger(Slf4jOperationalEventLogger.class);

    @Override
    public void emit(String eventType, String severity, UUID actorId, String endpoint, String outcome, String detail) {
        String message = "eventType=" + safe(eventType) +
                " severity=" + safe(severity).toUpperCase(Locale.ROOT) +
                " correlationId=" + safe(MDC.get("correlationId")) +
                " actorId=" + (actorId == null ? "anonymous" : actorId) +
                " endpoint=" + safe(endpoint) +
                " outcome=" + safe(outcome) +
                " detail=" + safe(detail);

        String level = safe(severity).toUpperCase(Locale.ROOT);
        switch (level) {
            case "ERROR" -> log.error(message);
            case "WARN" -> log.warn(message);
            default -> log.info(message);
        }
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "n/a";
        }
        return value.replaceAll("\\s+", "_");
    }
}
