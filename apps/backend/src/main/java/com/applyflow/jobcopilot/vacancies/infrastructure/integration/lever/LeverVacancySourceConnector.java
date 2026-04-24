package com.applyflow.jobcopilot.vacancies.infrastructure.integration.lever;

import com.applyflow.jobcopilot.shared.application.exception.BadRequestException;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.ExternalVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancySourceConfiguration;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancySourceConnector;
import com.applyflow.jobcopilot.vacancies.infrastructure.integration.IngestionEndpointSecurityValidator;
import com.applyflow.jobcopilot.vacancies.infrastructure.integration.IngestionProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class LeverVacancySourceConnector implements VacancySourceConnector {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final IngestionProperties properties;

    public LeverVacancySourceConnector(@Qualifier("leverRestClient") RestClient leverRestClient,
                                       ObjectMapper objectMapper,
                                       IngestionProperties properties) {
        this.restClient = leverRestClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public VacancyIngestionSource source() {
        return VacancyIngestionSource.LEVER;
    }

    @Override
    public List<ExternalVacancyRecord> fetch(VacancySourceConfiguration configuration, int requestedLimit) {
        IngestionProperties.Lever cfg = properties.getSources().getLever();
        IngestionEndpointSecurityValidator.validateHttpsAndAllowlist(cfg.getBaseUrl(), cfg.getAllowedHosts(), "lever");

        String site = textConfig(configuration, "site");
        if (site == null || site.isBlank()) {
            throw new BadRequestException("Fonte Lever sem site configurado");
        }
        String tenant = textConfig(configuration, "tenant");
        if (tenant == null || tenant.isBlank()) {
            tenant = site;
        }

        int maxItems = effectiveLimit(cfg.getMaxJobsPerRun(), requestedLimit,
                intConfig(configuration, "maxJobsPerRun", cfg.getMaxJobsPerRun()));

        byte[] payload = fetchWithRetry("/v0/postings/" + site + "?mode=json");
        if (payload == null || payload.length == 0) {
            throw new BadRequestException("Fonte Lever retornou payload vazio");
        }
        if (payload.length > cfg.getMaxPayloadBytes()) {
            throw new BadRequestException("Payload Lever excede limite operacional");
        }

        try {
            JsonNode jobs = objectMapper.readTree(payload);
            if (!jobs.isArray()) {
                return List.of();
            }
            List<ExternalVacancyRecord> output = new ArrayList<>();
            for (JsonNode job : jobs) {
                if (output.size() >= maxItems) {
                    break;
                }
                if (!isPublished(job)) {
                    continue;
                }
                String externalId = text(job, "id");
                if (externalId == null || externalId.isBlank()) {
                    continue;
                }
                String description = text(job, "descriptionPlain");
                if (description == null || description.isBlank()) {
                    description = text(job, "description");
                }
                String location = text(job.path("categories"), "location");
                String commitment = text(job.path("categories"), "commitment");

                output.add(new ExternalVacancyRecord(
                        VacancyIngestionSource.LEVER,
                        tenant,
                        externalId,
                        text(job, "hostedUrl"),
                        inferRemoteType(location, description),
                        commitment,
                        text(job, "text"),
                        site,
                        location,
                        isRemote(location, description),
                        inferSeniority(text(job, "text"), description),
                        List.of(),
                        description,
                        description,
                        parseDate(text(job, "updatedAt")),
                        job
                ));
            }
            return output;
        } catch (Exception ex) {
            throw new BadRequestException("Falha ao parsear payload Lever");
        }
    }

    private boolean isPublished(JsonNode job) {
        if (job.path("state").isTextual()) {
            return "published".equalsIgnoreCase(job.path("state").asText())
                    || "listed".equalsIgnoreCase(job.path("state").asText());
        }
        return job.path("hostedUrl").isTextual();
    }

    private byte[] fetchWithRetry(String path) {
        Exception last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return restClient.get().uri(path).accept(MediaType.APPLICATION_JSON).retrieve().body(byte[].class);
            } catch (Exception ex) {
                last = ex;
                sleepBackoff(attempt);
            }
        }
        throw new BadRequestException("Falha ao consultar Lever: " + (last == null ? "erro desconhecido" : last.getClass().getSimpleName()));
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

    private boolean isRemote(String location, String description) {
        String value = ((location == null ? "" : location) + " " + (description == null ? "" : description)).toLowerCase();
        return value.contains("remote");
    }

    private String inferRemoteType(String location, String description) {
        return isRemote(location, description) ? "REMOTE" : "ONSITE";
    }

    private String inferSeniority(String title, String description) {
        String joined = ((title == null ? "" : title) + " " + (description == null ? "" : description)).toLowerCase();
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
            if (raw.matches("^\\d+$")) {
                return OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(Long.parseLong(raw)), java.time.ZoneOffset.UTC);
            }
            return OffsetDateTime.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }
}
