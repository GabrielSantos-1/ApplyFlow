package com.applyflow.jobcopilot.vacancies.infrastructure.integration;

import com.applyflow.jobcopilot.shared.application.exception.BadRequestException;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class IngestionEndpointSecurityValidator {
    private IngestionEndpointSecurityValidator() {
    }

    public static void validateHttpsAndAllowlist(String baseUrl, List<String> allowedHosts, String sourceName) {
        URI uri = URI.create(baseUrl);
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BadRequestException("Base URL da fonte " + sourceName + " invalida");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new BadRequestException("Fonte " + sourceName + " deve usar HTTPS");
        }
        Set<String> allowed = allowedHosts.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!allowed.contains(host.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Host da fonte " + sourceName + " fora da allowlist");
        }
    }
}
