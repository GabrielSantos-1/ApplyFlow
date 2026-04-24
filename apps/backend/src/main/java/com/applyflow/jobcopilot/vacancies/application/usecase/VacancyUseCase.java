package com.applyflow.jobcopilot.vacancies.application.usecase;

import com.applyflow.jobcopilot.shared.application.dto.PageResponse;
import com.applyflow.jobcopilot.vacancies.application.dto.request.VacancySearchRequest;
import com.applyflow.jobcopilot.vacancies.application.dto.response.VacancyResponse;

import java.util.UUID;

public interface VacancyUseCase {
    PageResponse<VacancyResponse> list(VacancySearchRequest request);
    VacancyResponse getById(UUID id);
}
