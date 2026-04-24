package com.applyflow.jobcopilot.shared.application.audit;

import com.applyflow.jobcopilot.shared.application.observability.OperationalEventLogger;
import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import com.applyflow.jobcopilot.shared.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.applyflow.jobcopilot.shared.infrastructure.persistence.repository.AuditLogJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuditEventService {
    private static final Logger log = LoggerFactory.getLogger(AuditEventService.class);

    private final AuditLogJpaRepository repository;
    private final OperationalMetricsService operationalMetricsService;
    private final OperationalEventLogger operationalEventLogger;
    private final ObjectMapper objectMapper;

    public AuditEventService(AuditLogJpaRepository repository,
                             OperationalMetricsService operationalMetricsService,
                             OperationalEventLogger operationalEventLogger,
                             ObjectMapper objectMapper) {
        this.repository = repository;
        this.operationalMetricsService = operationalMetricsService;
        this.operationalEventLogger = operationalEventLogger;
        this.objectMapper = objectMapper;
    }

    public void log(UUID actorUserId, String action, String resource, UUID resourceId, String beforeState, String afterState) {
        try {
            AuditLogJpaEntity entity = new AuditLogJpaEntity();
            entity.setId(UUID.randomUUID());
            entity.setActorUserId(actorUserId);
            entity.setAction(action);
            entity.setResource(resource);
            entity.setResourceId(resourceId);
            entity.setCorrelationId(MDC.get("correlationId"));
            entity.setBeforeState(toJsonNode(beforeState));
            entity.setAfterState(toJsonNode(afterState));
            entity.setCreatedAt(OffsetDateTime.now());
            entity.setUpdatedAt(OffsetDateTime.now());
            repository.save(entity);
        } catch (RuntimeException ex) {
            // Audit trail failure must be explicit in logs/metrics, but cannot break protected flows.
            operationalMetricsService.recordAuditLogPersistenceFailure(action);
            operationalEventLogger.emit("audit_log_persist_failed", "ERROR", actorUserId, "n/a", "failed", action);
            log.error("audit.persist.failed action={} resource={} reason={}", action, resource, ex.toString());
        }
    }

    private JsonNode toJsonNode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception ignored) {
            return TextNode.valueOf(value);
        }
    }
}
