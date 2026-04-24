package com.applyflow.jobcopilot.vacancies.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record AdminVacancyIngestionRunRequest(
        @Pattern(regexp = "(?i)greenhouse|lever|remotive|adzuna")
        String sourceType,
        UUID sourceConfigId,
        @Min(1) @Max(500)
        Integer limit
) {
}
