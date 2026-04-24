package com.applyflow.jobcopilot.applications.domain;

import com.applyflow.jobcopilot.shared.domain.BaseEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ApplicationDraft extends BaseEntity {
    private final UUID userId;
    private final UUID vacancyId;
    private final UUID resumeVariantId;
    private final String messageDraft;
    private final ApplicationStatus status;

    public ApplicationDraft(UUID id, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID userId, UUID vacancyId, UUID resumeVariantId, String messageDraft, ApplicationStatus status) {
        super(id, createdAt, updatedAt);
        this.userId = userId;
        this.vacancyId = vacancyId;
        this.resumeVariantId = resumeVariantId;
        this.messageDraft = messageDraft;
        this.status = status;
    }

    public UUID getUserId() { return userId; }
    public UUID getVacancyId() { return vacancyId; }
    public UUID getResumeVariantId() { return resumeVariantId; }
    public String getMessageDraft() { return messageDraft; }
    public ApplicationStatus getStatus() { return status; }
}
