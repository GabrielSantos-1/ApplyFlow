package com.applyflow.jobcopilot.shared.domain;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class AuditLog extends BaseEntity {
    private final UUID actorUserId;
    private final String action;
    private final String resource;
    private final UUID resourceId;
    private final String correlationId;
    private final Map<String, Object> beforeState;
    private final Map<String, Object> afterState;

    public AuditLog(UUID id, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID actorUserId, String action, String resource,
                    UUID resourceId, String correlationId, Map<String, Object> beforeState, Map<String, Object> afterState) {
        super(id, createdAt, updatedAt);
        this.actorUserId = actorUserId;
        this.action = action;
        this.resource = resource;
        this.resourceId = resourceId;
        this.correlationId = correlationId;
        this.beforeState = beforeState;
        this.afterState = afterState;
    }

    public UUID getActorUserId() { return actorUserId; }
    public String getAction() { return action; }
    public String getResource() { return resource; }
    public UUID getResourceId() { return resourceId; }
    public String getCorrelationId() { return correlationId; }
    public Map<String, Object> getBeforeState() { return beforeState; }
    public Map<String, Object> getAfterState() { return afterState; }
}
