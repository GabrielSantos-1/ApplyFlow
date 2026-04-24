package com.applyflow.jobcopilot.shared.application.audit;

import com.applyflow.jobcopilot.shared.application.observability.OperationalEventLogger;
import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import com.applyflow.jobcopilot.shared.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.applyflow.jobcopilot.shared.infrastructure.persistence.repository.AuditLogJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditEventServiceTest {

    @Test
    void shouldPersistAuditStatesAsJsonNodes() {
        AuditLogJpaRepository repository = mock(AuditLogJpaRepository.class);
        OperationalMetricsService metrics = mock(OperationalMetricsService.class);
        OperationalEventLogger logger = mock(OperationalEventLogger.class);
        when(repository.save(org.mockito.ArgumentMatchers.any(AuditLogJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditEventService service = new AuditEventService(repository, metrics, logger, new ObjectMapper());
        UUID actorId = UUID.randomUUID();

        service.log(actorId, "ACTION_TEST", "resource_test", null, "{\"from\":\"old\"}", "updated");

        ArgumentCaptor<AuditLogJpaEntity> entityCaptor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
        verify(repository, times(1)).save(entityCaptor.capture());
        AuditLogJpaEntity persisted = entityCaptor.getValue();

        assertEquals("old", persisted.getBeforeState().get("from").asText());
        assertEquals("updated", persisted.getAfterState().asText());
        assertNull(persisted.getResourceId());
    }

    @Test
    void shouldNotBreakRequestFlowWhenAuditPersistenceFails() {
        AuditLogJpaRepository repository = mock(AuditLogJpaRepository.class);
        OperationalMetricsService metrics = mock(OperationalMetricsService.class);
        OperationalEventLogger logger = mock(OperationalEventLogger.class);
        doThrow(new IllegalStateException("db-down"))
                .when(repository)
                .save(org.mockito.ArgumentMatchers.any(AuditLogJpaEntity.class));

        AuditEventService service = new AuditEventService(repository, metrics, logger, new ObjectMapper());

        assertDoesNotThrow(() ->
                service.log(UUID.randomUUID(), "RATE_LIMIT_UNAVAILABLE", "http_endpoint", null, null, "POST:/api/v1/auth/login"));

        verify(metrics, times(1)).recordAuditLogPersistenceFailure("RATE_LIMIT_UNAVAILABLE");
        verify(logger, times(1)).emit(org.mockito.ArgumentMatchers.eq("audit_log_persist_failed"), org.mockito.ArgumentMatchers.eq("ERROR"),
                org.mockito.ArgumentMatchers.any(UUID.class),
                org.mockito.ArgumentMatchers.eq("n/a"),
                org.mockito.ArgumentMatchers.eq("failed"),
                org.mockito.ArgumentMatchers.eq("RATE_LIMIT_UNAVAILABLE"));
    }
}
