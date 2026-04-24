package com.applyflow.jobcopilot.vacancies.application.ingestion.contract;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VacancyIngestionExecutionResult(
        UUID runId,
        VacancyIngestionSource source,
        UUID sourceConfigId,
        IngestionTriggerType triggerType,
        String status,
        int fetchedCount,
        int normalizedCount,
        int insertedCount,
        int updatedCount,
        int skippedCount,
        int failedCount,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String errorSummary
) {
}
