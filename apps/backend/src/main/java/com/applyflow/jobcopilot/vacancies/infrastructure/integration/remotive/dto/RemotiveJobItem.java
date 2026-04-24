package com.applyflow.jobcopilot.vacancies.infrastructure.integration.remotive.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RemotiveJobItem(
        Long id,
        String url,
        String title,
        @JsonProperty("company_name")
        String companyName,
        @JsonProperty("candidate_required_location")
        String candidateRequiredLocation,
        @JsonProperty("job_type")
        String jobType,
        List<String> tags,
        @JsonProperty("published_date")
        String publishedDate,
        String description
) {
}
