package com.applyflow.jobcopilot.ai.application.service;

import com.applyflow.jobcopilot.shared.application.security.TextSanitizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class AiOutputValidator {
    private static final Set<String> FORBIDDEN_SNIPPETS = Set.of(
            "ignore previous instructions",
            "system prompt",
            "developer message",
            "tool call",
            "<script"
    );

    private final ObjectMapper objectMapper;
    private final TextSanitizer sanitizer;

    public AiOutputValidator(ObjectMapper objectMapper, TextSanitizer sanitizer) {
        this.objectMapper = objectMapper;
        this.sanitizer = sanitizer;
    }

    public ValidatedMatchEnrichment parseMatchEnrichment(String raw) {
        JsonNode node = readRoot(raw);
        String summary = clean(readRequiredText(node, "summary"), 700);
        List<String> strengths = cleanList(readRequiredArray(node, "strengths"), 5, 160, false);
        List<String> gaps = cleanList(readRequiredArray(node, "gaps"), 5, 160, true);
        List<String> nextSteps = cleanList(readRequiredArray(node, "nextSteps"), 5, 160, true);
        return new ValidatedMatchEnrichment(summary, strengths, gaps, nextSteps);
    }

    public ValidatedCvImprovement parseCvImprovement(String raw) {
        JsonNode node = readRoot(raw);
        List<String> improvements = cleanList(readRequiredArray(node, "improvementSuggestions"), 8, 120, true);
        List<String> atsKeywords = cleanList(readRequiredArray(node, "atsKeywords"), 10, 50, true);
        List<String> highlights = cleanList(readRequiredArray(node, "highlightPoints"), 8, 120, true);
        List<String> gapActions = cleanList(readRequiredArray(node, "gapActions"), 8, 120, true);
        return new ValidatedCvImprovement(improvements, atsKeywords, highlights, gapActions);
    }

    public ValidatedApplicationDraft parseApplicationDraft(String raw) {
        JsonNode node = readRoot(raw);
        String shortMessage = clean(readRequiredText(node, "shortMessage"), 280);
        String miniCoverNote = clean(readRequiredText(node, "miniCoverNote"), 500);
        return new ValidatedApplicationDraft(shortMessage, miniCoverNote);
    }

    private JsonNode readRoot(String raw) {
        try {
            String candidate = extractJsonObject(raw);
            JsonNode root = objectMapper.readTree(candidate);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("Resposta de IA sem objeto JSON valido");
            }
            return root;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Resposta de IA invalida para schema esperado", ex);
        }
    }

    private String extractJsonObject(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        return trimmed;
    }

    private String readRequiredText(JsonNode node, String key) {
        JsonNode value = node.get(key);
        if (value == null || value.isNull() || !value.isTextual()) {
            throw new IllegalArgumentException("Campo obrigatorio ausente/invalido: " + key);
        }
        String raw = value.asText();
        if (raw.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatorio vazio: " + key);
        }
        return raw;
    }

    private JsonNode readRequiredArray(JsonNode node, String key) {
        JsonNode value = node.get(key);
        if (value == null || !value.isArray()) {
            throw new IllegalArgumentException("Campo obrigatorio ausente/invalido: " + key);
        }
        return value;
    }

    private List<String> cleanList(JsonNode arrayNode, int maxItems, int maxLen, boolean requireNonEmpty) {
        List<String> out = new ArrayList<>();
        int limit = Math.min(arrayNode.size(), maxItems);
        for (int i = 0; i < limit; i++) {
            JsonNode item = arrayNode.get(i);
            if (item == null || !item.isTextual()) {
                continue;
            }
            String cleaned = clean(item.asText(), maxLen);
            if (!cleaned.isBlank()) {
                out.add(cleaned);
            }
        }
        if (requireNonEmpty && out.isEmpty()) {
            throw new IllegalArgumentException("Lista obrigatoria sem itens validos");
        }
        return dedupe(out);
    }

    private String clean(String value, int maxLen) {
        String cleaned = sanitizer.sanitizeFreeText(value, maxLen).trim();
        String lower = cleaned.toLowerCase(Locale.ROOT);
        for (String forbidden : FORBIDDEN_SNIPPETS) {
            if (lower.contains(forbidden)) {
                throw new IllegalArgumentException("Conteudo potencialmente inseguro na resposta de IA");
            }
        }
        return cleaned;
    }

    private List<String> dedupe(List<String> values) {
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    public record ValidatedMatchEnrichment(
            String summary,
            List<String> strengths,
            List<String> gaps,
            List<String> nextSteps
    ) {
    }

    public record ValidatedCvImprovement(
            List<String> improvementSuggestions,
            List<String> atsKeywords,
            List<String> highlightPoints,
            List<String> gapActions
    ) {
    }

    public record ValidatedApplicationDraft(
            String shortMessage,
            String miniCoverNote
    ) {
    }
}
