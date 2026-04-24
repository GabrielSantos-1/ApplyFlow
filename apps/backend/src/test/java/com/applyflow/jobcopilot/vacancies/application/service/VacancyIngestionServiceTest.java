package com.applyflow.jobcopilot.vacancies.application.service;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.ExternalVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.IngestionTriggerType;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.NormalizedVacancyRecord;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionExecutionResult;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionRunView;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancySourceConfiguration;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.IngestionExecutionLock;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.IngestionRunRepository;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancyIngestionMetrics;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancyIngestionRepository;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancySourceConfigurationRepository;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancySourceConnector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VacancyIngestionServiceTest {

    @Test
    void shouldRunPipelineAndReturnSuccessCounters() {
        VacancySourceConnector connector = mock(VacancySourceConnector.class);
        VacancyIngestionNormalizer normalizer = mock(VacancyIngestionNormalizer.class);
        VacancyIngestionRepository repository = mock(VacancyIngestionRepository.class);
        VacancySourceConfigurationRepository sourceConfigurationRepository = mock(VacancySourceConfigurationRepository.class);
        IngestionExecutionLock lock = mock(IngestionExecutionLock.class);
        IngestionRunRepository runRepository = mock(IngestionRunRepository.class);
        VacancyIngestionMetrics metrics = mock(VacancyIngestionMetrics.class);

        VacancySourceConfiguration cfg = config(VacancyIngestionSource.REMOTIVE);
        when(sourceConfigurationRepository.findEnabledBySource(VacancyIngestionSource.REMOTIVE)).thenReturn(List.of(cfg));
        when(connector.source()).thenReturn(VacancyIngestionSource.REMOTIVE);
        when(connector.fetch(any(), anyInt())).thenReturn(List.of(raw("1"), raw("2")));
        when(normalizer.normalize(any())).thenReturn(normalized("1"), normalized("2"));
        when(repository.upsert(any())).thenReturn(
                VacancyIngestionRepository.UpsertOutcome.INSERTED,
                VacancyIngestionRepository.UpsertOutcome.DUPLICATE_UNCHANGED
        );
        when(lock.tryAcquire(VacancyIngestionSource.REMOTIVE)).thenReturn(true);

        UUID runId = UUID.randomUUID();
        when(runRepository.createRun(any(), any(), any(), any(), any(), any())).thenReturn(runId);
        when(runRepository.finishRun(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(), any(), any()))
                .thenAnswer(invocation -> new VacancyIngestionExecutionResult(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3),
                        invocation.getArgument(4),
                        invocation.getArgument(5),
                        invocation.getArgument(6),
                        invocation.getArgument(7),
                        invocation.getArgument(8),
                        invocation.getArgument(9),
                        invocation.getArgument(10),
                        invocation.getArgument(11),
                        invocation.getArgument(12),
                        invocation.getArgument(13)
                ));
        when(runRepository.listRuns(any(Pageable.class))).thenReturn(Page.empty());

        VacancyIngestionService service = new VacancyIngestionService(
                List.of(connector), normalizer, repository, sourceConfigurationRepository, lock, runRepository, metrics
        );

        VacancyIngestionExecutionResult result = service.run(VacancyIngestionSource.REMOTIVE, IngestionTriggerType.MANUAL);

        assertEquals("SUCCESS", result.status());
        assertEquals(2, result.fetchedCount());
        assertEquals(2, result.normalizedCount());
        assertEquals(1, result.insertedCount());
        assertEquals(1, result.skippedCount());
        assertEquals(0, result.failedCount());
        verify(lock).release(VacancyIngestionSource.REMOTIVE);
    }

    @Test
    void shouldSkipWhenLockIsUnavailable() {
        VacancySourceConnector connector = mock(VacancySourceConnector.class);
        VacancyIngestionNormalizer normalizer = mock(VacancyIngestionNormalizer.class);
        VacancyIngestionRepository repository = mock(VacancyIngestionRepository.class);
        VacancySourceConfigurationRepository sourceConfigurationRepository = mock(VacancySourceConfigurationRepository.class);
        IngestionExecutionLock lock = mock(IngestionExecutionLock.class);
        IngestionRunRepository runRepository = mock(IngestionRunRepository.class);
        VacancyIngestionMetrics metrics = mock(VacancyIngestionMetrics.class);

        VacancySourceConfiguration cfg = config(VacancyIngestionSource.REMOTIVE);
        when(sourceConfigurationRepository.findEnabledBySource(VacancyIngestionSource.REMOTIVE)).thenReturn(List.of(cfg));
        when(connector.source()).thenReturn(VacancyIngestionSource.REMOTIVE);
        when(lock.tryAcquire(VacancyIngestionSource.REMOTIVE)).thenReturn(false);
        when(runRepository.createRun(any(), any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());
        when(runRepository.finishRun(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(), any(), any()))
                .thenAnswer(invocation -> new VacancyIngestionExecutionResult(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3),
                        invocation.getArgument(4),
                        invocation.getArgument(5),
                        invocation.getArgument(6),
                        invocation.getArgument(7),
                        invocation.getArgument(8),
                        invocation.getArgument(9),
                        invocation.getArgument(10),
                        invocation.getArgument(11),
                        invocation.getArgument(12),
                        invocation.getArgument(13)
                ));
        when(runRepository.listRuns(any(Pageable.class))).thenReturn(Page.empty());

        VacancyIngestionService service = new VacancyIngestionService(
                List.of(connector), normalizer, repository, sourceConfigurationRepository, lock, runRepository, metrics
        );

        VacancyIngestionExecutionResult result = service.run(VacancyIngestionSource.REMOTIVE, IngestionTriggerType.SCHEDULED);

        assertEquals("SKIPPED_LOCKED", result.status());
        verify(connector, never()).fetch(any(), anyInt());
        verify(lock, never()).release(VacancyIngestionSource.REMOTIVE);
    }

    private VacancySourceConfiguration config(VacancyIngestionSource source) {
        return new VacancySourceConfiguration(
                UUID.randomUUID(),
                source,
                source.name(),
                new ObjectMapper().createObjectNode().put("tenant", source.name().toLowerCase()),
                true
        );
    }

    private ExternalVacancyRecord raw(String id) {
        return new ExternalVacancyRecord(
                VacancyIngestionSource.REMOTIVE,
                "remotive.com",
                id,
                "https://remotive.com/jobs/" + id,
                "REMOTE",
                "FULL_TIME",
                "Backend " + id,
                "ApplyFlow",
                "Remote",
                true,
                "senior",
                List.of("Java"),
                "Desc " + id,
                "Desc " + id,
                OffsetDateTime.now(),
                new ObjectMapper().createObjectNode().put("id", id)
        );
    }

    private NormalizedVacancyRecord normalized(String id) {
        return new NormalizedVacancyRecord(
                VacancyIngestionNormalizer.REMOTIVE_PLATFORM_ID,
                VacancyIngestionSource.REMOTIVE,
                "remotive.com",
                id,
                "https://remotive.com/jobs/" + id,
                "checksum-" + id,
                "Backend " + id,
                ("backend " + id),
                "ApplyFlow",
                "applyflow",
                "Remote",
                "remote",
                "REMOTE",
                "FULL_TIME",
                true,
                "senior",
                "senior",
                List.of("Java"),
                80,
                List.of(),
                "t:backend " + id + "|c:applyflow|l:remote|r:true|s:senior|k:java",
                "Desc " + id,
                "Desc " + id,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                new ObjectMapper().createObjectNode().put("id", id),
                new ObjectMapper().createObjectNode().put("id", id)
        );
    }
}
