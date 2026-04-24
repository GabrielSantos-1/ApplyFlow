package com.applyflow.jobcopilot.vacancies.application.ingestion.port;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AdminIngestionOverviewRepository {
    List<SourceOverviewRow> listSources();

    List<RunTotalsRow> aggregateRunTotals();

    List<LastRunRow> latestRuns();

    List<VacancyAggregateRow> aggregateVacancies(OffsetDateTime since24h, OffsetDateTime since7d);

    List<QualityFlagRow> topQualityFlags(int limit);

    record SourceOverviewRow(
            UUID sourceConfigId,
            String name,
            String sourceType,
            String tenant,
            boolean active
    ) {
    }

    record RunTotalsRow(
            UUID sourceConfigId,
            long fetchedCount,
            long persistedCount,
            long skippedCount,
            long failedCount
    ) {
    }

    record LastRunRow(
            UUID sourceConfigId,
            String status,
            long durationMs,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            long fetchedCount,
            long insertedCount,
            long updatedCount,
            long skippedCount,
            long failedCount
    ) {
    }

    record VacancyAggregateRow(
            String source,
            String sourceTenant,
            long totalCount,
            long duplicateCount,
            double averageQualityScore,
            long recent24hCount,
            long recent7dCount
    ) {
    }

    record QualityFlagRow(
            String flag,
            long count
    ) {
    }
}
