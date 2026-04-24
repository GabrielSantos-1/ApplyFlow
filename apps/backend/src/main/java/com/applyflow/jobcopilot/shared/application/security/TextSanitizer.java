package com.applyflow.jobcopilot.shared.application.security;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Component
public class TextSanitizer {
    private static final Pattern CONTROL_AND_BIDI = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]|[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069]");
    private static final Pattern MULTI_WHITESPACE = Pattern.compile("\\s{2,}");

    public String sanitizeFreeText(String raw) {
        return sanitizeFreeText(raw, 4000);
    }

    public String sanitizeFreeText(String raw, int maxLength) {
        if (raw == null) {
            return null;
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        String cleaned = CONTROL_AND_BIDI.matcher(normalized).replaceAll("");
        cleaned = MULTI_WHITESPACE.matcher(cleaned).replaceAll(" ").trim();
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        return escapeHtml(cleaned);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
