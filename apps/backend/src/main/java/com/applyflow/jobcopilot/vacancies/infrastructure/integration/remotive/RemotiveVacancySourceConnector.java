package com.applyflow.jobcopilot.vacancies.infrastructure.integration.remotive;

import com.applyflow.jobcopilot.shared.application.exception.BadRequestException;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.ExternalVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.JobSearchCriteria;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancySourceConfiguration;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancySourceConnector;
import com.applyflow.jobcopilot.vacancies.infrastructure.integration.IngestionProperties;
import com.applyflow.jobcopilot.vacancies.infrastructure.integration.remotive.dto.RemotiveApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RemotiveVacancySourceConnector implements VacancySourceConnector {
    private static final int MAX_CATEGORY_REQUESTS_PER_RUN = 4;
    private static final long BACKOFF_AFTER_TWO_REQUESTS_MS = 61_000L;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final IngestionProperties properties;
    private final RemotivePayloadMapper mapper;

    public RemotiveVacancySourceConnector(@Qualifier("remotiveRestClient") RestClient remotiveRestClient,
                                          ObjectMapper objectMapper,
                                          IngestionProperties properties,
                                          RemotivePayloadMapper mapper) {
        this.restClient = remotiveRestClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public VacancyIngestionSource source() {
        return VacancyIngestionSource.REMOTIVE;
    }

    @Override
    public List<ExternalVacancyRecord> fetch(VacancySourceConfiguration configuration, int requestedLimit) {
        IngestionProperties.Remotive cfg = properties.getSources().getRemotive();
        validateTarget(cfg);
        int limit = resolveLimit(configuration, cfg.getMaxJobsPerRun(), requestedLimit);
        String tenant = resolveTenant(configuration, "remotive.com");
        String jobsPath = resolveJobsPath(configuration, cfg.getJobsPath());

        List<String> categories = resolveCategories(configuration);
        if (categories.isEmpty()) {
            return fetchPage(jobsPath, limit, tenant, cfg.getMaxPayloadBytes(), null);
        }

        Map<String, ExternalVacancyRecord> unique = new LinkedHashMap<>();
        int requestCount = 0;
        for (String category : categories) {
            if (unique.size() >= limit || requestCount >= MAX_CATEGORY_REQUESTS_PER_RUN) {
                break;
            }
            if (requestCount > 0 && requestCount % 2 == 0) {
                sleepRateLimitBackoff();
            }
            int remaining = limit - unique.size();
            List<ExternalVacancyRecord> records = fetchPage(jobsPath, remaining, tenant, cfg.getMaxPayloadBytes(), category);
            for (ExternalVacancyRecord record : records) {
                if (record.externalJobId() != null && !record.externalJobId().isBlank()) {
                    unique.putIfAbsent(record.externalJobId(), record);
                }
                if (unique.size() >= limit) {
                    break;
                }
            }
            requestCount++;
        }
        return new ArrayList<>(unique.values());
    }

    @Override
    public List<ExternalVacancyRecord> fetchSearch(VacancySourceConfiguration configuration,
                                                   JobSearchCriteria criteria,
                                                   int requestedLimit) {
        IngestionProperties.Remotive cfg = properties.getSources().getRemotive();
        validateTarget(cfg);
        int limit = Math.min(resolveLimit(configuration, cfg.getMaxJobsPerRun(), requestedLimit), 50);
        String tenant = resolveTenant(configuration, "remotive.com");
        String jobsPath = resolveJobsPath(configuration, cfg.getJobsPath());
        return fetchPage(jobsPath, limit, tenant, cfg.getMaxPayloadBytes(), null, criteria.normalizedKeyword());
    }

    private List<ExternalVacancyRecord> fetchPage(String jobsPath,
                                                  int limit,
                                                  String tenant,
                                                  int maxPayloadBytes,
                                                  String category) {
        return fetchPage(jobsPath, limit, tenant, maxPayloadBytes, category, null);
    }

    private List<ExternalVacancyRecord> fetchPage(String jobsPath,
                                                  int limit,
                                                  String tenant,
                                                  int maxPayloadBytes,
                                                  String category,
                                                  String search) {
        byte[] payload = fetchWithRetry(jobsPath, limit, category, search);

        if (payload == null || payload.length == 0) {
            throw new BadRequestException("Fonte remotive retornou payload vazio");
        }
        if (payload.length > maxPayloadBytes) {
            throw new BadRequestException("Payload remotive excede limite operacional");
        }

        try {
            RemotiveApiResponse response = objectMapper.readValue(payload, RemotiveApiResponse.class);
            return mapper.toExternalRecords(response, limit, tenant);
        } catch (Exception ex) {
            throw new BadRequestException("Falha ao parsear payload da fonte remotive");
        }
    }

    private byte[] fetchWithRetry(String jobsPath, int limit, String category, String search) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(jobsPath)
                .replaceQueryParam("limit", limit);
        if (category != null && !category.isBlank()) {
            builder.replaceQueryParam("category", category);
        }
        if (search != null && !search.isBlank()) {
            builder.replaceQueryParam("search", search);
        }
        String requestPath = builder.build().toUriString();
        Exception last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return restClient.get()
                        .uri(requestPath)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(byte[].class);
            } catch (Exception ex) {
                last = ex;
                try {
                    Thread.sleep(150L * attempt);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new BadRequestException("Falha ao consultar fonte remotive: " + (last == null ? "erro desconhecido" : last.getClass().getSimpleName()));
    }

    private void validateTarget(IngestionProperties.Remotive cfg) {
        URI uri = URI.create(cfg.getBaseUrl());
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BadRequestException("Base URL da fonte remotive invalida");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new BadRequestException("Fonte remotive deve usar HTTPS");
        }
        Set<String> allowed = cfg.getAllowedHosts().stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!allowed.contains(host.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Host da fonte remotive fora da allowlist");
        }
    }

    private int resolveLimit(VacancySourceConfiguration configuration, int defaultLimit, int requestedLimit) {
        int fromConfig = configuration.configJson() != null && configuration.configJson().has("maxJobsPerRun")
                ? configuration.configJson().path("maxJobsPerRun").asInt(defaultLimit)
                : defaultLimit;
        int effective = Math.min(fromConfig, requestedLimit > 0 ? requestedLimit : fromConfig);
        return Math.max(1, effective);
    }

    private String resolveTenant(VacancySourceConfiguration configuration, String fallback) {
        String tenant = configuration.configJson() != null
                ? configuration.configJson().path("tenant").asText(fallback)
                : fallback;
        return tenant == null || tenant.isBlank() ? fallback : tenant;
    }

    private String resolveJobsPath(VacancySourceConfiguration configuration, String fallback) {
        String configured = configuration.configJson() != null
                ? configuration.configJson().path("jobsPath").asText(fallback)
                : fallback;
        if (configured == null || configured.isBlank() || !configured.startsWith("/")) {
            return fallback;
        }
        return configured;
    }

    private List<String> resolveCategories(VacancySourceConfiguration configuration) {
        if (configuration.configJson() == null || !configuration.configJson().has("categories")) {
            return List.of();
        }
        if (!configuration.configJson().path("categories").isArray()) {
            return List.of();
        }
        List<String> categories = new ArrayList<>();
        configuration.configJson().path("categories").forEach(node -> {
            String value = node.asText(null);
            if (value != null && value.matches("^[a-z0-9-]{2,60}$")) {
                categories.add(value);
            }
        });
        return categories.stream()
                .distinct()
                .limit(MAX_CATEGORY_REQUESTS_PER_RUN)
                .toList();
    }

    private void sleepRateLimitBackoff() {
        try {
            Thread.sleep(BACKOFF_AFTER_TWO_REQUESTS_MS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
