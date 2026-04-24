package com.applyflow.jobcopilot.ai.infrastructure.provider;

import com.applyflow.jobcopilot.ai.application.exception.AiProviderException;
import com.applyflow.jobcopilot.ai.application.port.AiTextGenerationPort;
import com.applyflow.jobcopilot.ai.domain.AiFlow;
import com.applyflow.jobcopilot.ai.infrastructure.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OpenAiCompatibleTextProvider implements AiTextGenerationPort {
    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    public OpenAiCompatibleTextProvider(@Qualifier("aiRestClient") RestClient aiRestClient,
                                        ObjectMapper objectMapper,
                                        AiProperties properties) {
        this.aiRestClient = aiRestClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public AiTextGenerationResult generate(AiFlow flow, String systemPrompt, String userPrompt) {
        if (!properties.isEnabled()) {
            throw new AiProviderException("provider-disabled");
        }
        AiProperties.Provider cfg = properties.getProvider();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new AiProviderException("missing-api-key");
        }
        validateTarget(cfg);

        int attempts = Math.max(0, cfg.getMaxRetries()) + 1;
        Exception lastFailure = null;
        for (int i = 1; i <= attempts; i++) {
            try {
                Map<String, Object> request = new LinkedHashMap<>();
                request.put("model", cfg.getModel());
                request.put("temperature", cfg.getTemperature());
                request.put("max_tokens", cfg.getMaxCompletionTokens());
                request.put("response_format", Map.of("type", "json_object"));
                request.put("messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ));

                byte[] body = aiRestClient.post()
                        .uri(cfg.getChatPath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + cfg.getApiKey())
                        .body(request)
                        .retrieve()
                        .body(byte[].class);

                if (body == null || body.length == 0) {
                    throw new AiProviderException("empty-response");
                }
                JsonNode root = objectMapper.readTree(body);
                JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
                if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                    throw new AiProviderException("missing-content");
                }
                String model = root.path("model").asText(cfg.getModel());
                JsonNode usageNode = root.path("usage");
                Integer promptTokens = readOptionalInt(usageNode, "prompt_tokens");
                Integer completionTokens = readOptionalInt(usageNode, "completion_tokens");
                Integer totalTokens = readOptionalInt(usageNode, "total_tokens");
                return new AiTextGenerationResult(contentNode.asText(), model, promptTokens, completionTokens, totalTokens);
            } catch (AiProviderException ex) {
                lastFailure = ex;
            } catch (Exception ex) {
                lastFailure = ex;
                String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
                if (msg.contains("timed out")) {
                    throw new AiProviderException("timeout", ex);
                }
            }
        }
        throw new AiProviderException("provider-call-failed", lastFailure);
    }

    @Override
    public String providerName() {
        return properties.getProvider().getName();
    }

    private void validateTarget(AiProperties.Provider cfg) {
        URI uri = URI.create(cfg.getBaseUrl());
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new AiProviderException("invalid-provider-base-url");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new AiProviderException("provider-must-use-https");
        }
        Set<String> allowed = cfg.getAllowedHosts().stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!allowed.contains(host.toLowerCase(Locale.ROOT))) {
            throw new AiProviderException("provider-host-not-allowed");
        }
    }

    private Integer readOptionalInt(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.path(field);
        if (!value.isIntegralNumber()) {
            return null;
        }
        return value.asInt();
    }
}
