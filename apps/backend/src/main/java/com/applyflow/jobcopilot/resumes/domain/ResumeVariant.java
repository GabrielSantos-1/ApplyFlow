package com.applyflow.jobcopilot.resumes.domain;

import com.applyflow.jobcopilot.shared.domain.BaseEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ResumeVariant extends BaseEntity {
    private final UUID resumeId;
    private final UUID vacancyId;
    private final String variantLabel;
    private final ResumeVariantStatus status;

    public ResumeVariant(UUID id, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID resumeId, UUID vacancyId, String variantLabel, ResumeVariantStatus status) {
        super(id, createdAt, updatedAt);
        this.resumeId = resumeId;
        this.vacancyId = vacancyId;
        this.variantLabel = variantLabel;
        this.status = status;
    }

    public UUID getResumeId() { return resumeId; }
    public UUID getVacancyId() { return vacancyId; }
    public String getVariantLabel() { return variantLabel; }
    public ResumeVariantStatus getStatus() { return status; }
}
