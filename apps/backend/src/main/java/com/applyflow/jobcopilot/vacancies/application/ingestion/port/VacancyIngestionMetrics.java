package com.applyflow.jobcopilot.vacancies.application.ingestion.port;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;

import java.time.Duration;

public interface VacancyIngestionMetrics {
    void recordRunStarted(VacancyIngestionSource source, String triggerType);

    void recordRunCompleted(VacancyIngestionSource source, String status, Duration duration);

    void recordStageFailure(VacancyIngestionSource source, String stage);

    void recordSkipped(VacancyIngestionSource source);

    void recordInserted(VacancyIngestionSource source);

    void recordUpdated(VacancyIngestionSource source);
}
