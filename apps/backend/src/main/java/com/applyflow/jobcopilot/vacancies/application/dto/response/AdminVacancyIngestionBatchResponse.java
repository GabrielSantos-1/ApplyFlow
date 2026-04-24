package com.applyflow.jobcopilot.vacancies.application.dto.response;

import java.util.List;

public record AdminVacancyIngestionBatchResponse(
        int requestedRuns,
        int successfulRuns,
        int failedRuns,
        List<VacancyIngestionRunResponse> runs
) {
}
