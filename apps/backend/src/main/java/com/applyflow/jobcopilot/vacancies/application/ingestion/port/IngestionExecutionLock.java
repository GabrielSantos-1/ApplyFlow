package com.applyflow.jobcopilot.vacancies.application.ingestion.port;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;

public interface IngestionExecutionLock {
    boolean tryAcquire(VacancyIngestionSource source);

    void release(VacancyIngestionSource source);
}
