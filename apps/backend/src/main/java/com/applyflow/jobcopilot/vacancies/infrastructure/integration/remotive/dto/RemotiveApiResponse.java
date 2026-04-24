package com.applyflow.jobcopilot.vacancies.infrastructure.integration.remotive.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RemotiveApiResponse(
        List<RemotiveJobItem> jobs
) {
}
