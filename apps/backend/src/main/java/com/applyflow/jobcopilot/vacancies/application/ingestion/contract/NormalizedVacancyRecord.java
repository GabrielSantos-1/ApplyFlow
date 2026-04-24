package com.applyflow.jobcopilot.vacancies.application.ingestion.contract;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record NormalizedVacancyRecord(
        UUID platformId,
        VacancyIngestionSource source,
        String sourceTenant,
        String externalJobId,
        String sourceUrl,
        String checksum,
        String title,
        String canonicalTitle,
        String companyName,
        String canonicalCompanyName,
        String location,
        String canonicalLocation,
        String remoteType,
        String employmentType,
        boolean remote,
        String seniority,
        String normalizedSeniority,
        List<String> requiredSkills,
        int qualityScore,
        List<String> qualityFlags,
        String dedupeKey,
        String descriptionRaw,
        String requirements,
        OffsetDateTime discoveredAt,
        OffsetDateTime publishedAt,
        JsonNode normalizedPayload,
        JsonNode rawPayload
) {
}
