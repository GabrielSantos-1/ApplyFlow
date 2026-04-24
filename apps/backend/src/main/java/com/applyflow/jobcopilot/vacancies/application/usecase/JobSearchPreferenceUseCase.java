package com.applyflow.jobcopilot.vacancies.application.usecase;

import com.applyflow.jobcopilot.vacancies.application.dto.request.CreateJobSearchPreferenceRequest;
import com.applyflow.jobcopilot.vacancies.application.dto.request.UpdateJobSearchPreferenceRequest;
import com.applyflow.jobcopilot.vacancies.application.dto.response.JobSearchPreferenceResponse;

import java.util.List;
import java.util.UUID;

public interface JobSearchPreferenceUseCase {
    List<JobSearchPreferenceResponse> list();

    JobSearchPreferenceResponse create(CreateJobSearchPreferenceRequest request);

    JobSearchPreferenceResponse update(UUID id, UpdateJobSearchPreferenceRequest request);
}
