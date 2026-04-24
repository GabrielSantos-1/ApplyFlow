package com.applyflow.jobcopilot.shared.application.observability;

import java.util.UUID;

public interface OperationalEventLogger {
    void emit(String eventType,
              String severity,
              UUID actorId,
              String endpoint,
              String outcome,
              String detail);
}
