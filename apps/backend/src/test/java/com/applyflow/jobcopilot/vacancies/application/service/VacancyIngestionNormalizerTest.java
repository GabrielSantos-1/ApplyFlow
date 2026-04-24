package com.applyflow.jobcopilot.vacancies.application.service;

import com.applyflow.jobcopilot.shared.application.security.TextSanitizer;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.ExternalVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.NormalizedVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VacancyIngestionNormalizerTest {
    private final VacancyIngestionNormalizer normalizer =
            new VacancyIngestionNormalizer(new TextSanitizer(), new ObjectMapper());

    @Test
    void shouldNormalizeRemotivePayload() {
        ExternalVacancyRecord raw = baseRecord(VacancyIngestionSource.REMOTIVE, "remotive.com", "123", "<script>Senior Backend</script>");

        NormalizedVacancyRecord normalized = normalizer.normalize(raw);

        assertEquals(VacancyIngestionNormalizer.REMOTIVE_PLATFORM_ID, normalized.platformId());
        assertEquals("123", normalized.externalJobId());
        assertFalse(normalized.title().contains("<script>"));
        assertEquals("senior backend", normalized.canonicalTitle());
        assertEquals("applyflow", normalized.canonicalCompanyName());
        assertEquals("remote", normalized.canonicalLocation());
        assertEquals("senior", normalized.normalizedSeniority());
        assertTrue(normalized.qualityScore() > 0);
        assertNotNull(normalized.dedupeKey());
        assertEquals("REMOTIVE", normalized.normalizedPayload().get("source").asText());
        assertEquals(64, normalized.checksum().length());
    }

    @Test
    void shouldNormalizeGreenhousePayload() {
        ExternalVacancyRecord raw = baseRecord(VacancyIngestionSource.GREENHOUSE, "board-abc", "g-1", "Senior Java");

        NormalizedVacancyRecord normalized = normalizer.normalize(raw);

        assertEquals(VacancyIngestionNormalizer.GREENHOUSE_PLATFORM_ID, normalized.platformId());
        assertEquals("board-abc", normalized.sourceTenant());
        assertEquals("g-1", normalized.externalJobId());
    }

    @Test
    void shouldNormalizeLeverPayload() {
        ExternalVacancyRecord raw = baseRecord(VacancyIngestionSource.LEVER, "site-xyz", "l-9", "Pleno Backend");

        NormalizedVacancyRecord normalized = normalizer.normalize(raw);

        assertEquals(VacancyIngestionNormalizer.LEVER_PLATFORM_ID, normalized.platformId());
        assertEquals("site-xyz", normalized.sourceTenant());
        assertEquals("l-9", normalized.externalJobId());
    }

    private ExternalVacancyRecord baseRecord(VacancyIngestionSource source, String tenant, String externalId, String title) {
        return new ExternalVacancyRecord(
                source,
                tenant,
                externalId,
                "https://example.com/job/" + externalId,
                "REMOTE",
                "FULL_TIME",
                title,
                "ApplyFlow <b>Inc</b>",
                "Brasil",
                true,
                "senior",
                List.of("Java", "<i>Spring</i>"),
                "<p>Descricao com <script>alert(1)</script></p>",
                "<p>Descricao com <script>alert(1)</script></p>",
                OffsetDateTime.parse("2026-04-21T12:00:00Z"),
                new ObjectMapper().createObjectNode().put("id", externalId)
        );
    }
}
