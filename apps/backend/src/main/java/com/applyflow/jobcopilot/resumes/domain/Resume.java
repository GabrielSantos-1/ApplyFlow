package com.applyflow.jobcopilot.resumes.domain;

import com.applyflow.jobcopilot.shared.domain.BaseEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Resume extends BaseEntity {
    private final UUID userId;
    private final String title;
    private final String sourceFileName;
    private final ResumeStatus status;

    public Resume(UUID id, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID userId, String title, String sourceFileName, ResumeStatus status) {
        super(id, createdAt, updatedAt);
        this.userId = userId;
        this.title = title;
        this.sourceFileName = sourceFileName;
        this.status = status;
    }

    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getSourceFileName() { return sourceFileName; }
    public ResumeStatus getStatus() { return status; }
}
