package com.applyflow.jobcopilot.vacancies.infrastructure.integration.remotive;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.ExternalVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.infrastructure.integration.remotive.dto.RemotiveApiResponse;
import com.applyflow.jobcopilot.vacancies.infrastructure.integration.remotive.dto.RemotiveJobItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class RemotivePayloadMapper {
    private final ObjectMapper objectMapper;

    public RemotivePayloadMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ExternalVacancyRecord> toExternalRecords(RemotiveApiResponse response, int maxItems, String sourceTenant) {
        List<ExternalVacancyRecord> output = new ArrayList<>();
        List<RemotiveJobItem> jobs = response != null && response.jobs() != null ? response.jobs() : List.of();
        for (RemotiveJobItem job : jobs) {
            if (output.size() >= maxItems) {
                break;
            }
            output.add(new ExternalVacancyRecord(
                    VacancyIngestionSource.REMOTIVE,
                    sourceTenant,
                    job.id() == null ? null : String.valueOf(job.id()),
                    job.url(),
                    isRemote(job.jobType(), job.candidateRequiredLocation()) ? "REMOTE" : "ONSITE",
                    null,
                    job.title(),
                    job.companyName(),
                    job.candidateRequiredLocation(),
                    isRemote(job.jobType(), job.candidateRequiredLocation()),
                    inferSeniority(job.tags()),
                    job.tags() == null ? List.of() : job.tags(),
                    job.description(),
                    job.description(),
                    parseDate(job.publishedDate()),
                    safeRawPayload(job)
            ));
        }
        return output;
    }

    private JsonNode safeRawPayload(RemotiveJobItem job) {
        JsonNode node = objectMapper.valueToTree(job);
        if (node instanceof ObjectNode objectNode) {
            objectNode.remove("description");
            objectNode.put("descriptionOmitted", true);
        }
        return node;
    }

    private boolean isRemote(String jobType, String location) {
        String value = (jobType == null ? "" : jobType) + " " + (location == null ? "" : location);
        return value.toLowerCase().contains("remote");
    }

    private String inferSeniority(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        String joined = String.join(" ", tags).toLowerCase();
        if (joined.contains("junior") || joined.contains("jr")) {
            return "junior";
        }
        if (joined.contains("senior") || joined.contains("sr")) {
            return "senior";
        }
        if (joined.contains("lead") || joined.contains("principal") || joined.contains("staff")) {
            return "especialista";
        }
        return null;
    }

    private OffsetDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
