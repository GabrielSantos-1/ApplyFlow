package com.applyflow.jobcopilot.applications.domain;

import com.applyflow.jobcopilot.shared.domain.BaseEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ApplicationTracking extends BaseEntity {
    private final UUID applicationDraftId;
    private final TrackingStage stage;
    private final String notes;

    public ApplicationTracking(UUID id, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID applicationDraftId, TrackingStage stage, String notes) {
        super(id, createdAt, updatedAt);
        this.applicationDraftId = applicationDraftId;
        this.stage = stage;
        this.notes = notes;
    }

    public UUID getApplicationDraftId() { return applicationDraftId; }
    public TrackingStage getStage() { return stage; }
    public String getNotes() { return notes; }
}
