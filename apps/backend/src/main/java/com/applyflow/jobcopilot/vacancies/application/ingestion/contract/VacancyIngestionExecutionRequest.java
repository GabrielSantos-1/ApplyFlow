package com.applyflow.jobcopilot.vacancies.application.ingestion.contract;

public record VacancyIngestionExecutionRequest(
        VacancyIngestionSource source,
        IngestionTriggerType triggerType
) {
}
