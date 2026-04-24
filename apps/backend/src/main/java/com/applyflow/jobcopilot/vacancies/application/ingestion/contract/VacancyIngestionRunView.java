package com.applyflow.jobcopilot.vacancies.application.ingestion.contract;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VacancyIngestionRunView(
        UUID runId,
        VacancyIngestionSource sourceType,
        UUID sourceConfigId,
        IngestionTriggerType triggerType,
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
