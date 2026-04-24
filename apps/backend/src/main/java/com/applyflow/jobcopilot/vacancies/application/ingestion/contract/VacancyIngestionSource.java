package com.applyflow.jobcopilot.vacancies.application.ingestion.contract;

public enum VacancyIngestionSource {
    GREENHOUSE,
    LEVER,
    REMOTIVE,
    ADZUNA;

    public static VacancyIngestionSource fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("sourceType obrigatorio");
        }
        return VacancyIngestionSource.valueOf(raw.trim().toUpperCase());
    }
}
