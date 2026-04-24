package com.applyflow.jobcopilot.shared.interfaces.http;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        String errorCode,
        String message,
        String correlationId
) {
}
