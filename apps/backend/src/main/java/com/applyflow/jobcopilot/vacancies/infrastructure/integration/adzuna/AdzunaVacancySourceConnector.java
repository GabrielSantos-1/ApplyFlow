package com.applyflow.jobcopilot.vacancies.infrastructure.integration.adzuna;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.ExternalVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.JobSearchCriteria;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancySourceConfiguration;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancySourceConnector;
import com.applyflow.jobcopilot.vacancies.infrastructure.integration.IngestionEndpointSecurityValidator;
import com.applyflow.jobcopilot.vacancies.infrastructure.integration.IngestionProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class AdzunaVacancySourceConnector implements VacancySourceConnector {
    private static final Logger log = LoggerFactory.getLogger(AdzunaVacancySourceConnector.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final IngestionProperties properties;

    public AdzunaVacancySourceConnector(@Qualifier("adzunaRestClient") RestClient adzunaRestClient,
                                        ObjectMapper objectMapper,
                                        IngestionProperties properties) {
        this.restClient = adzunaRestClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public VacancyIngestionSource source() {
        return VacancyIngestionSource.ADZUNA;
    }

    @Override
    public List<ExternalVacancyRecord> fetch(VacancySourceConfiguration configuration, int requestedLimit) {
        return fetchInternal(configuration, requestedLimit, null);
    }

    @Override
    public List<ExternalVacancyRecord> fetchSearch(VacancySourceConfiguration configuration,
                                                   JobSearchCriteria criteria,
                                                   int requestedLimit) {
        return fetchInternal(configuration, Math.min(requestedLimit, 50), criteria);
    }

    private List<ExternalVacancyRecord> fetchInternal(VacancySourceConfiguration configuration,
                                                      int requestedLimit,
                                                      JobSearchCriteria criteria) {
        IngestionProperties.Adzuna cfg = properties.getSources().getAdzuna();
        IngestionEndpointSecurityValidator.validateHttpsAndAllowlist(cfg.getBaseUrl(), cfg.getAllowedHosts(), "adzuna");

        if (cfg.getAppId() == null || cfg.getAppId().isBlank() || cfg.getAppKey() == null || cfg.getAppKey().isBlank()) {
            log.warn("eventType=vacancy.ingestion stage=fetch source=ADZUNA outcome=skipped reason=missing_credentials");
            return List.of();
        }

        String country = textConfig(configuration, "country");
        if (country == null || country.isBlank()) {
            country = "us";
        }
        int page = intConfig(configuration, "page", 1);
        int resultsPerPage = intConfig(configuration, "resultsPerPage", 30);
        int maxItems = effectiveLimit(cfg.getMaxJobsPerRun(), requestedLimit,
                intConfig(configuration, "maxJobsPerRun", cfg.getMaxJobsPerRun()));
        String tenant = textConfig(configuration, "tenant");
        if (tenant == null || tenant.isBlank()) {
            tenant = country;
        }

        String uri = buildSearchPath(country, page, cfg, Math.min(resultsPerPage, maxItems), criteria);
        byte[] payload = fetchWithRetry(uri);
        if (payload == null || payload.length == 0) {
            return List.of();
        }
        if (payload.length > cfg.getMaxPayloadBytes()) {
            throw new IllegalStateException("Payload Adzuna excede limite operacional");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode results = root.path("results");
            if (!results.isArray()) {
                return List.of();
            }
            List<ExternalVacancyRecord> output = new ArrayList<>();
            for (JsonNode item : results) {
                if (output.size() >= maxItems) {
                    break;
                }
                String externalId = text(item, "id");
                if (externalId == null || externalId.isBlank()) {
                    continue;
                }
                String description = text(item, "description");
                String location = text(item.path("location"), "display_name");
                boolean remote = isRemote(location, description);
                String seniority = inferSeniority(text(item, "title"), description);
                if (criteria != null && criteria.remoteOnly() && !remote) {
                    continue;
                }
                if (criteria != null && criteria.seniority() != null && seniority != null
                        && !criteria.seniority().equalsIgnoreCase(seniority)) {
                    continue;
                }
                output.add(new ExternalVacancyRecord(
                        VacancyIngestionSource.ADZUNA,
                        tenant,
                        externalId,
                        text(item, "redirect_url"),
                        remote ? "REMOTE" : "ONSITE",
                        text(item.path("contract_type"), "display_name"),
                        text(item, "title"),
                        text(item.path("company"), "display_name"),
                        location,
                        remote,
                        seniority,
                        List.of(),
                        description,
                        description,
                        parseDate(text(item, "created")),
                        item
                ));
            }
            return output;
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao parsear payload Adzuna");
        }
    }

    private String buildSearchPath(String country,
                                   int page,
                                   IngestionProperties.Adzuna cfg,
                                   int resultsPerPage,
                                   JobSearchCriteria criteria) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/v1/api/jobs/{country}/search/{page}")
                .queryParam("app_id", cfg.getAppId())
                .queryParam("app_key", cfg.getAppKey())
                .queryParam("results_per_page", resultsPerPage)
                .queryParam("sort_by", "date");
        if (criteria != null && criteria.normalizedKeyword() != null && !criteria.normalizedKeyword().isBlank()) {
            builder.queryParam("what", criteria.normalizedKeyword());
        }
        if (criteria != null && criteria.location() != null && !criteria.location().isBlank()) {
            builder.queryParam("where", criteria.location());
        }
        return builder.build(country.toLowerCase(Locale.ROOT), page).toString();
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
        throw new IllegalStateException("Falha ao consultar Adzuna: " + (last == null ? "erro desconhecido" : last.getClass().getSimpleName()));
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
            return OffsetDateTime.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }
}
