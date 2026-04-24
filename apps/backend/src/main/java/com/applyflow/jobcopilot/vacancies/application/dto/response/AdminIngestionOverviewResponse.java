package com.applyflow.jobcopilot.vacancies.application.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminIngestionOverviewResponse(
        List<ProviderOverview> providers,
        TotalsOverview totals,
        QualityOverview quality,
        DedupeOverview dedupe,
        RecentOverview recent
) {
    public record ProviderOverview(
            UUID sourceConfigId,
            String name,
            String sourceType,
            String tenant,
            boolean active,
            long vacanciesCollected,
            long vacanciesPersisted,
            long duplicateVacancies,
            double averageQualityScore,
            LastExecutionOverview lastExecution
    ) {
    }

    public record LastExecutionOverview(
            String status,
            long durationMs,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            long fetchedCount,
            long persistedCount,
            long skippedCount,
            long failedCount
    ) {
    }

    public record TotalsOverview(
            long providers,
            long activeProviders,
            long vacanciesCollected,
            long vacanciesPersisted,
            long vacanciesTotal,
            long vacanciesVisible
    ) {
    }

    public record QualityOverview(
            double averageQualityScore,
            List<QualityFlagOverview> topFlags
    ) {
    }

    public record QualityFlagOverview(
            String flag,
            long count
    ) {
    }

    public record DedupeOverview(
            long totalVacancies,
            long duplicateVacancies,
            double duplicateRatePercent
    ) {
    }

    public record RecentOverview(
            long last24h,
            long last7d
    ) {
    }
}
