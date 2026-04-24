package com.applyflow.jobcopilot.shared.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public abstract class BaseEntity {
    private final UUID id;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected BaseEntity(UUID id, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void touch(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
