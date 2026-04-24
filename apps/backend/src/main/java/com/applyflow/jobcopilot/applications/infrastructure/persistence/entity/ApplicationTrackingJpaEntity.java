package com.applyflow.jobcopilot.applications.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "application_tracking")
public class ApplicationTrackingJpaEntity {
    @Id
    private UUID id;
    @Column(name = "application_draft_id")
    private UUID applicationDraftId;
    private String stage;
    private String notes;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getApplicationDraftId() { return applicationDraftId; }
    public void setApplicationDraftId(UUID applicationDraftId) { this.applicationDraftId = applicationDraftId; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}