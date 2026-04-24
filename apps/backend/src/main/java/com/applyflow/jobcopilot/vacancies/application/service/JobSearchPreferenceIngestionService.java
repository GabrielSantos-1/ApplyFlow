package com.applyflow.jobcopilot.vacancies.application.service;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.ExternalVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.JobSearchCriteria;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.NormalizedVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancySourceConfiguration;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.IngestionExecutionLock;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancyIngestionMetrics;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancyIngestionRepository;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancySourceConfigurationRepository;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancySourceConnector;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.UserJobSearchPreferenceJpaEntity;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.repository.UserJobSearchPreferenceJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class JobSearchPreferenceIngestionService {
    private static final Logger log = LoggerFactory.getLogger(JobSearchPreferenceIngestionService.class);
    private static final int MAX_PREFERENCES_PER_RUN = 20;
    private static final int MAX_JOBS_PER_PREFERENCE = 30;
    private static final long REQUEST_BACKOFF_MS = 500L;

    private final UserJobSearchPreferenceJpaRepository preferenceRepository;
    private final VacancySourceConfigurationRepository sourceConfigurationRepository;
    private final Map<VacancyIngestionSource, VacancySourceConnector> connectors;
    private final VacancyIngestionNormalizer normalizer;
    private final VacancyIngestionRepository ingestionRepository;
    private final IngestionExecutionLock executionLock;
    private final VacancyIngestionMetrics metrics;

    public JobSearchPreferenceIngestionService(UserJobSearchPreferenceJpaRepository preferenceRepository,
                                               VacancySourceConfigurationRepository sourceConfigurationRepository,
                                               List<VacancySourceConnector> connectorList,
                                               VacancyIngestionNormalizer normalizer,
                                               VacancyIngestionRepository ingestionRepository,
                                               IngestionExecutionLock executionLock,
                                               VacancyIngestionMetrics metrics) {
        this.preferenceRepository = preferenceRepository;
        this.sourceConfigurationRepository = sourceConfigurationRepository;
        this.connectors = new EnumMap<>(VacancyIngestionSource.class);
        connectorList.forEach(connector -> this.connectors.put(connector.source(), connector));
        this.normalizer = normalizer;
        this.ingestionRepository = ingestionRepository;
        this.executionLock = executionLock;
        this.metrics = metrics;
    }

    public void runActivePreferences() {
        List<UserJobSearchPreferenceJpaEntity> preferences = preferenceRepository.findByEnabledTrueOrderByUpdatedAtAsc()
                .stream()
                .limit(MAX_PREFERENCES_PER_RUN)
                .toList();
        for (UserJobSearchPreferenceJpaEntity preference : preferences) {
            executePreference(preference);
            sleepBackoff();
        }
    }

    private void executePreference(UserJobSearchPreferenceJpaEntity preference) {
        VacancyIngestionSource source = VacancyIngestionSource.fromValue(preference.getProvider());
        Optional<VacancySourceConfiguration> config = sourceConfigurationRepository.findEnabledBySource(source).stream().findFirst();
        VacancySourceConnector connector = connectors.get(source);
        if (config.isEmpty() || connector == null) {
            updateLastRun(preference, "SKIPPED", 0, 0, 0, 0, 0);
            return;
        }

        boolean lockAcquired = executionLock.tryAcquire(source);
        if (!lockAcquired) {
            updateLastRun(preference, "SKIPPED_LOCKED", 0, 0, 0, 0, 0);
            return;
        }

        int fetchedCount = 0;
        int insertedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        String status = "SUCCESS";

        try {
            JobSearchCriteria criteria = new JobSearchCriteria(
                    preference.getKeyword(),
                    preference.getNormalizedKeyword(),
                    preference.getLocation(),
                    preference.isRemoteOnly(),
                    preference.getSeniority()
            );
            List<ExternalVacancyRecord> externalRecords = connector.fetchSearch(config.get(), criteria, MAX_JOBS_PER_PREFERENCE);
            fetchedCount = externalRecords.size();

            for (ExternalVacancyRecord external : externalRecords) {
                try {
                    NormalizedVacancyRecord normalized = normalizer.normalize(external);
                    VacancyIngestionRepository.UpsertOutcome outcome = ingestionRepository.upsert(normalized);
                    if (outcome == VacancyIngestionRepository.UpsertOutcome.DUPLICATE_UNCHANGED) {
                        skippedCount++;
                        metrics.recordSkipped(source);
                    } else if (outcome == VacancyIngestionRepository.UpsertOutcome.UPDATED) {
                        updatedCount++;
                        metrics.recordUpdated(source);
                    } else {
                        insertedCount++;
                        metrics.recordInserted(source);
                    }
                } catch (Exception ex) {
                    failedCount++;
                    metrics.recordStageFailure(source, "preference_ingestion");
                    log.warn("eventType=job_search_preference.ingestion stage=record source={} preferenceId={} outcome=failed reason={}",
                            source.name(), preference.getId(), ex.getClass().getSimpleName());
                }
            }

            if (failedCount > 0 && (insertedCount > 0 || updatedCount > 0 || skippedCount > 0)) {
                status = "PARTIAL";
            } else if (failedCount > 0) {
                status = "FAILED";
            }
            log.info("eventType=job_search_preference.ingestion source={} preferenceId={} keywordHash={} outcome={} fetched={} inserted={} updated={} skipped={} failed={}",
                    source.name(), preference.getId(), hashKeyword(preference.getNormalizedKeyword()), status,
                    fetchedCount, insertedCount, updatedCount, skippedCount, failedCount);
        } catch (Exception ex) {
            failedCount++;
            status = "FAILED";
            metrics.recordStageFailure(source, "preference_fetch");
            log.warn("eventType=job_search_preference.ingestion stage=fetch source={} preferenceId={} keywordHash={} outcome=failed reason={}",
                    source.name(), preference.getId(), hashKeyword(preference.getNormalizedKeyword()), ex.getClass().getSimpleName());
        } finally {
            executionLock.release(source);
            updateLastRun(preference, status, fetchedCount, insertedCount, updatedCount, skippedCount, failedCount);
        }
    }

    private void updateLastRun(UserJobSearchPreferenceJpaEntity preference,
                               String status,
                               int fetched,
                               int inserted,
                               int updated,
                               int skipped,
                               int failed) {
        preference.setLastRunAt(OffsetDateTime.now());
        preference.setLastRunStatus(status);
        preference.setLastFetchedCount(fetched);
        preference.setLastInsertedCount(inserted);
        preference.setLastUpdatedCount(updated);
        preference.setLastSkippedCount(skipped);
        preference.setLastFailedCount(failed);
        preference.setUpdatedAt(OffsetDateTime.now());
        preferenceRepository.save(preference);
    }

    private void sleepBackoff() {
        try {
            Thread.sleep(REQUEST_BACKOFF_MS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private String hashKeyword(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 12);
        } catch (Exception ignored) {
            return "unavailable";
        }
    }
}
