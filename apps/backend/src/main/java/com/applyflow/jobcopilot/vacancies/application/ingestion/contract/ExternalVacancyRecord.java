package com.applyflow.jobcopilot.vacancies.application.ingestion.contract;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.List;

public record ExternalVacancyRecord(
        VacancyIngestionSource source,
        String sourceTenant,
        String externalJobId,
        String sourceUrl,
        String remoteType,
        String employmentType,
        String title,
        String company,
        String location,
        boolean remote,
        String seniority,
        List<String> requiredSkills,
        String rawDescription,
        String requirements,
        OffsetDateTime publishedAt,
        JsonNode rawPayload
) {
}
