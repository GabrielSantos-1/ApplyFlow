package com.applyflow.jobcopilot.applications.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "application_drafts")
public class ApplicationDraftJpaEntity {
    @Id
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "vacancy_id")
    private UUID vacancyId;
    @Column(name = "resume_variant_id")
    private UUID resumeVariantId;
    @Column(name = "message_draft")
    private String messageDraft;
    private String status;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getVacancyId() { return vacancyId; }
    public void setVacancyId(UUID vacancyId) { this.vacancyId = vacancyId; }
    public UUID getResumeVariantId() { return resumeVariantId; }
    public void setResumeVariantId(UUID resumeVariantId) { this.resumeVariantId = resumeVariantId; }
    public String getMessageDraft() { return messageDraft; }
    public void setMessageDraft(String messageDraft) { this.messageDraft = messageDraft; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}