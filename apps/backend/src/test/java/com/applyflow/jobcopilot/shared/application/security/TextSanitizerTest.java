package com.applyflow.jobcopilot.shared.application.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextSanitizerTest {
    private final TextSanitizer sanitizer = new TextSanitizer();

    @Test
    void shouldNormalizeAndEscapeMaliciousInput() {
        String raw = "  <script>alert(1)</script>\u0000\u202E  ";
        String result = sanitizer.sanitizeFreeText(raw, 50);

        assertTrue(result.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(result.contains("\u0000"));
        assertFalse(result.contains("\u202E"));
    }
}
