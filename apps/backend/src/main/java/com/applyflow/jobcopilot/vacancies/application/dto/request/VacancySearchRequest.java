package com.applyflow.jobcopilot.vacancies.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VacancySearchRequest(
        @Min(0) int page,
        @Min(1) @Max(100) int size,
        @Pattern(regexp = "(?i)createdAt|title|company") String sortBy,
        @Pattern(regexp = "(?i)asc|desc") String sortDirection,
        @Size(max = 100) String query,
        @Pattern(regexp = "(?i)remote|hybrid|onsite|all") String workModel,
        @Pattern(regexp = "(?i)junior|pleno|senior|especialista|all") String seniority
) {
}
