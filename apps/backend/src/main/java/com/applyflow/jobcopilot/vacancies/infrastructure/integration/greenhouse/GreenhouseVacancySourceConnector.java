package com.applyflow.jobcopilot.vacancies.infrastructure.integration.greenhouse;

import com.applyflow.jobcopilot.shared.application.exception.BadRequestException;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.ExternalVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancySourceConfiguration;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancySourceConnector;
import com.applyflow.jobcopilot.vacancies.infrastructure.integration.IngestionEndpointSecurityValidator;
import com.applyflow.jobcopilot.vacancies.infrastructure.integration.IngestionProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class GreenhouseVacancySourceConnector implements VacancySourceConnector {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final IngestionProperties properties;

    public GreenhouseVacancySourceConnector(@Qualifier("greenhouseRestClient") RestClient greenhouseRestClient,
                                            ObjectMapper objectMapper,
                                            IngestionProperties properties) {
        this.restClient = greenhouseRestClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public VacancyIngestionSource source() {
        return VacancyIngestionSource.GREENHOUSE;
    }

    @Override
    public List<ExternalVacancyRecord> fetch(VacancySourceConfiguration configuration, int requestedLimit) {
        IngestionProperties.Greenhouse cfg = properties.getSources().getGreenhouse();
        IngestionEndpointSecurityValidator.validateHttpsAndAllowlist(cfg.getBaseUrl(), cfg.getAllowedHosts(), "greenhouse");

        String boardToken = textConfig(configuration, "boardToken");
        if (boardToken == null || boardToken.isBlank()) {
            throw new BadRequestException("Fonte Greenhouse sem boardToken configurado");
        }
        if (!boardToken.matches("^[A-Za-z0-9_-]{2,80}$")) {
            throw new BadRequestException("Fonte Greenhouse com boardToken invalido");
        }

        String tenant = textConfig(configuration, "tenant");
        if (tenant == null || tenant.isBlank()) {
            tenant = boardToken;
        }

        int maxItems = effectiveLimit(cfg.getMaxJobsPerRun(), requestedLimit,
                intConfig(configuration, "maxJobsPerRun", cfg.getMaxJobsPerRun()));

        byte[] payload = fetchWithRetry("/v1/boards/" + boardToken + "/jobs?content=true");
        if (payload == null || payload.length == 0) {
            throw new BadRequestException("Fonte Greenhouse retornou payload vazio");
        }
        if (payload.length > cfg.getMaxPayloadBytes()) {
            throw new BadRequestException("Payload Greenhouse excede limite operacional");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            List<ExternalVacancyRecord> output = new ArrayList<>();
            JsonNode jobs = root.path("jobs");
            if (!jobs.isArray()) {
                return List.of();
            }
            for (JsonNode job : jobs) {
                if (output.size() >= maxItems) {
                    break;
                }
                String externalId = text(job, "id");
                if (externalId == null || externalId.isBlank()) {
                    continue;
                }
                String url = text(job, "absolute_url");
                String title = text(job, "title");
                String company = text(root, "company_name");
                if (company == null || company.isBlank()) {
                    company = tenant;
                }
                String location = text(job.path("location"), "name");
                String content = text(job, "content");
                output.add(new ExternalVacancyRecord(
                        VacancyIngestionSource.GREENHOUSE,
                        tenant,
                        externalId,
                        url,
                        inferRemoteType(location, content),
                        null,
                        title,
                        company,
                        location,
                        isRemote(location, content),
                        inferSeniority(title, content),
                        List.of(),
                        content,
                        content,
                        parseDate(text(job, "updated_at")),
                        safeRawPayload(job)
                ));
            }
            return output;
        } catch (Exception ex) {
            throw new BadRequestException("Falha ao parsear payload Greenhouse");
        }
    }

    private byte[] fetchWithRetry(String path) {
        Exception last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return restClient.get()
                        .uri(path)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(byte[].class);
            } catch (Exception ex) {
                last = ex;
                sleepBackoff(attempt);
            }
        }
        throw new BadRequestException("Falha ao consultar Greenhouse: " + (last == null ? "erro desconhecido" : last.getClass().getSimpleName()));
    }

    private JsonNode safeRawPayload(JsonNode job) {
        JsonNode node = job.deepCopy();
        if (node instanceof ObjectNode objectNode) {
            objectNode.remove("content");
            objectNode.put("contentOmitted", true);
        }
        return node;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return null;
        }
        return node.path(field).asText();
    }

    private String textConfig(VacancySourceConfiguration cfg, String field) {
        return cfg.configJson() == null ? null : cfg.configJson().path(field).asText(null);
    }

    private int intConfig(VacancySourceConfiguration cfg, String field, int defaultValue) {
        return cfg.configJson() != null && cfg.configJson().has(field)
                ? cfg.configJson().path(field).asInt(defaultValue)
                : defaultValue;
    }

    private int effectiveLimit(int sourceDefault, int requestedLimit, int configLimit) {
        int basis = Math.min(sourceDefault, configLimit);
        int requested = requestedLimit > 0 ? requestedLimit : basis;
        return Math.max(1, Math.min(requested, basis));
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(200L * attempt);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isRemote(String location, String content) {
        String value = ((location == null ? "" : location) + " " + (content == null ? "" : content)).toLowerCase();
        return value.contains("remote");
    }

    private String inferRemoteType(String location, String content) {
        return isRemote(location, content) ? "REMOTE" : "ONSITE";
    }

    private String inferSeniority(String title, String content) {
        String joined = ((title == null ? "" : title) + " " + (content == null ? "" : content)).toLowerCase();
        if (joined.contains("junior") || joined.contains("jr")) return "junior";
        if (joined.contains("senior") || joined.contains("sr")) return "senior";
        if (joined.contains("lead") || joined.contains("principal") || joined.contains("staff")) return "especialista";
        if (joined.contains("mid") || joined.contains("pleno")) return "pleno";
        return null;
    }

    private OffsetDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }
}
