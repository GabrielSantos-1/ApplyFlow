package com.applyflow.jobcopilot.vacancies.application.dto.response;

import com.applyflow.jobcopilot.vacancies.domain.VacancyStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record VacancyResponse(
        UUID id,
        String title,
        String company,
        String location,
        boolean remote,
        String seniority,
        String jobUrl,
        OffsetDateTime publishedAt,
        VacancyStatus status,
        List<String> requiredSkills
) {
}
