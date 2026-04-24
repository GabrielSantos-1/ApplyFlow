package com.applyflow.jobcopilot.vacancies.application.service;

import com.applyflow.jobcopilot.vacancies.application.dto.response.AdminIngestionOverviewResponse;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.AdminIngestionOverviewRepository;
import com.applyflow.jobcopilot.vacancies.application.usecase.AdminIngestionOverviewUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminIngestionOverviewService implements AdminIngestionOverviewUseCase {
    private static final Logger log = LoggerFactory.getLogger(AdminIngestionOverviewService.class);
    private static final int MAX_QUALITY_FLAGS = 5;

    private final AdminIngestionOverviewRepository overviewRepository;

    public AdminIngestionOverviewService(AdminIngestionOverviewRepository overviewRepository) {
        this.overviewRepository = overviewRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminIngestionOverviewResponse overview() {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime since24h = now.minusHours(24);
            OffsetDateTime since7d = now.minusDays(7);

            List<AdminIngestionOverviewRepository.SourceOverviewRow> sources = overviewRepository.listSources();
            Map<UUID, AdminIngestionOverviewRepository.RunTotalsRow> runTotals = totalsByConfigId(overviewRepository.aggregateRunTotals());
            Map<UUID, AdminIngestionOverviewRepository.LastRunRow> lastRuns = latestByConfigId(overviewRepository.latestRuns());
            Map<String, AdminIngestionOverviewRepository.VacancyAggregateRow> vacancyAggregates = bySourceTenant(
                    overviewRepository.aggregateVacancies(since24h, since7d)
            );

            List<AdminIngestionOverviewResponse.ProviderOverview> providers = sources.stream()
                    .map(source -> providerOverview(source, runTotals.get(source.sourceConfigId()), lastRuns.get(source.sourceConfigId()), vacancyAggregates.get(sourceKey(source))))
                    .sorted(Comparator.comparing(AdminIngestionOverviewResponse.ProviderOverview::sourceType)
                            .thenComparing(AdminIngestionOverviewResponse.ProviderOverview::name))
                    .toList();

            long vacanciesTotal = vacancyAggregates.values().stream()
                    .mapToLong(AdminIngestionOverviewRepository.VacancyAggregateRow::totalCount)
                    .sum();
            long duplicateVacancies = providers.stream().mapToLong(AdminIngestionOverviewResponse.ProviderOverview::duplicateVacancies).sum();
            long visibleVacancies = Math.max(0, vacanciesTotal - duplicateVacancies);
            long vacanciesCollected = providers.stream().mapToLong(AdminIngestionOverviewResponse.ProviderOverview::vacanciesCollected).sum();
            long vacanciesPersisted = providers.stream().mapToLong(AdminIngestionOverviewResponse.ProviderOverview::vacanciesPersisted).sum();
            long recent24h = vacancyAggregates.values().stream().mapToLong(AdminIngestionOverviewRepository.VacancyAggregateRow::recent24hCount).sum();
            long recent7d = vacancyAggregates.values().stream().mapToLong(AdminIngestionOverviewRepository.VacancyAggregateRow::recent7dCount).sum();

            double averageQuality = weightedAverageQuality(vacancyAggregates.values().stream().toList());
            List<AdminIngestionOverviewResponse.QualityFlagOverview> flags = overviewRepository.topQualityFlags(MAX_QUALITY_FLAGS)
                    .stream()
                    .map(flag -> new AdminIngestionOverviewResponse.QualityFlagOverview(flag.flag(), flag.count()))
                    .toList();

            return new AdminIngestionOverviewResponse(
                    providers,
                    new AdminIngestionOverviewResponse.TotalsOverview(
                            providers.size(),
                            providers.stream().filter(AdminIngestionOverviewResponse.ProviderOverview::active).count(),
                            vacanciesCollected,
                            vacanciesPersisted,
                            vacanciesTotal,
                            visibleVacancies
                    ),
                    new AdminIngestionOverviewResponse.QualityOverview(round(averageQuality), flags),
                    new AdminIngestionOverviewResponse.DedupeOverview(
                            vacanciesTotal,
                            duplicateVacancies,
                            percentage(duplicateVacancies, vacanciesTotal)
                    ),
                    new AdminIngestionOverviewResponse.RecentOverview(recent24h, recent7d)
            );
        } catch (RuntimeException ex) {
            log.error("eventType=admin.ingestion.overview outcome=failed reason={}:{}",
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    private AdminIngestionOverviewResponse.ProviderOverview providerOverview(AdminIngestionOverviewRepository.SourceOverviewRow source,
                                                                             AdminIngestionOverviewRepository.RunTotalsRow runTotals,
                                                                             AdminIngestionOverviewRepository.LastRunRow lastRun,
                                                                             AdminIngestionOverviewRepository.VacancyAggregateRow vacancies) {
        long collected = runTotals == null ? 0 : runTotals.fetchedCount();
        long persisted = runTotals == null ? 0 : runTotals.persistedCount();
        long duplicates = vacancies == null ? 0 : vacancies.duplicateCount();
        double quality = vacancies == null ? 0 : vacancies.averageQualityScore();

        return new AdminIngestionOverviewResponse.ProviderOverview(
                source.sourceConfigId(),
                source.name(),
                source.sourceType(),
                source.tenant(),
                source.active(),
                collected,
                persisted,
                duplicates,
                round(quality),
                lastRunOverview(lastRun)
        );
    }

    private AdminIngestionOverviewResponse.LastExecutionOverview lastRunOverview(AdminIngestionOverviewRepository.LastRunRow run) {
        if (run == null) {
            return null;
        }
        return new AdminIngestionOverviewResponse.LastExecutionOverview(
                normalizeStatus(run.status()),
                Math.max(0, run.durationMs()),
                run.startedAt(),
                run.finishedAt(),
                run.fetchedCount(),
                run.insertedCount() + run.updatedCount(),
                run.skippedCount(),
                run.failedCount()
        );
    }

    private Map<UUID, AdminIngestionOverviewRepository.RunTotalsRow> totalsByConfigId(List<AdminIngestionOverviewRepository.RunTotalsRow> rows) {
        Map<UUID, AdminIngestionOverviewRepository.RunTotalsRow> output = new HashMap<>();
        rows.forEach(row -> output.put(row.sourceConfigId(), row));
        return output;
    }

    private Map<UUID, AdminIngestionOverviewRepository.LastRunRow> latestByConfigId(List<AdminIngestionOverviewRepository.LastRunRow> rows) {
        Map<UUID, AdminIngestionOverviewRepository.LastRunRow> output = new HashMap<>();
        rows.forEach(row -> output.put(row.sourceConfigId(), row));
        return output;
    }

    private Map<String, AdminIngestionOverviewRepository.VacancyAggregateRow> bySourceTenant(List<AdminIngestionOverviewRepository.VacancyAggregateRow> rows) {
        Map<String, AdminIngestionOverviewRepository.VacancyAggregateRow> output = new HashMap<>();
        rows.forEach(row -> output.put(key(row.source(), row.sourceTenant()), row));
        return output;
    }

    private String sourceKey(AdminIngestionOverviewRepository.SourceOverviewRow source) {
        return key(source.sourceType(), source.tenant());
    }

    private String key(String source, String tenant) {
        return (source == null ? "" : source) + "|" + (tenant == null ? "" : tenant);
    }

    private double weightedAverageQuality(List<AdminIngestionOverviewRepository.VacancyAggregateRow> rows) {
        long total = rows.stream().mapToLong(AdminIngestionOverviewRepository.VacancyAggregateRow::totalCount).sum();
        if (total == 0) {
            return 0;
        }
        double weighted = rows.stream()
                .mapToDouble(row -> row.averageQualityScore() * row.totalCount())
                .sum();
        return weighted / total;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNKNOWN";
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            return "FAIL";
        }
        return status;
    }

    private double percentage(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return round((value * 100.0) / total);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
