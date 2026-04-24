package com.applyflow.jobcopilot.vacancies.application.ingestion.contract;

public record JobSearchCriteria(
        String keyword,
        String normalizedKeyword,
        String location,
        boolean remoteOnly,
        String seniority
) {
}
