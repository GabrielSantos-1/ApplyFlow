package com.applyflow.jobcopilot.vacancies.application.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VacancyIngestionRunRequest(
        @Size(max = 30)
        @Pattern(regexp = "(?i)greenhouse|lever|remotive|adzuna")
        String source
) {
}
