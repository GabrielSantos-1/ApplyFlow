package com.applyflow.jobcopilot.vacancies.application.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VacancyIngestionRunResponse(
        UUID runId,
        String sourceType,
        UUID sourceConfigId,
        String triggerType,
        String status,
        int fetchedCount,
        int normalizedCount,
        int insertedCount,
        int updatedCount,
        int skippedCount,
        int failedCount,
        String triggeredBy,
        String correlationId,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String errorSummary
) {
}
