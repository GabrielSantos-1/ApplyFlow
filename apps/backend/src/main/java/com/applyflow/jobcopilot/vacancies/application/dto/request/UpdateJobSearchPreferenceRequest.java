package com.applyflow.jobcopilot.vacancies.application.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateJobSearchPreferenceRequest(
        @Size(min = 2, max = 80) String keyword,
        @Size(max = 80) String location,
        Boolean remoteOnly,
        @Size(max = 30) String seniority,
        @Size(max = 20) String provider,
        Boolean enabled
) {
}
