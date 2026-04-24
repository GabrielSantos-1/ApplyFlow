package com.applyflow.jobcopilot.vacancies.application.ingestion.contract;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public record VacancySourceConfiguration(
        UUID id,
        VacancyIngestionSource sourceType,
        String displayName,
        JsonNode configJson,
        boolean enabled
) {
}
