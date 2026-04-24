package com.applyflow.jobcopilot.vacancies.application.ingestion.port;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.NormalizedVacancyRecord;

public interface VacancyIngestionRepository {

    UpsertOutcome upsert(NormalizedVacancyRecord record);

    enum UpsertOutcome {
        INSERTED,
        UPDATED,
        DUPLICATE_UNCHANGED
    }
}
