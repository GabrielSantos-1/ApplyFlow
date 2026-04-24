package com.applyflow.jobcopilot.vacancies.application.usecase;

import com.applyflow.jobcopilot.shared.application.dto.PageResponse;
import com.applyflow.jobcopilot.vacancies.application.dto.request.AdminVacancyIngestionRunRequest;
import com.applyflow.jobcopilot.vacancies.application.dto.response.AdminVacancyIngestionBatchResponse;
import com.applyflow.jobcopilot.vacancies.application.dto.response.VacancyIngestionRunResponse;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.IngestionTriggerType;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionExecutionResult;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.shared.application.security.AuthenticatedUser;

public interface VacancyIngestionUseCase {
    VacancyIngestionExecutionResult run(VacancyIngestionSource source, IngestionTriggerType triggerType);

    AdminVacancyIngestionBatchResponse runAdmin(AdminVacancyIngestionRunRequest request, AuthenticatedUser actor, String correlationId);

    PageResponse<VacancyIngestionRunResponse> listRuns(int page, int size);
}
