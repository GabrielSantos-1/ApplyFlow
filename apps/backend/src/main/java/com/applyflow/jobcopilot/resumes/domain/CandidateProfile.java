package com.applyflow.jobcopilot.resumes.domain;

import com.applyflow.jobcopilot.shared.domain.BaseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class CandidateProfile extends BaseEntity {
    private final UUID userId;
    private final String headline;
    private final String summary;
    private final String location;
    private final List<String> primarySkills;

    public CandidateProfile(UUID id, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID userId, String headline, String summary, String location, List<String> primarySkills) {
        super(id, createdAt, updatedAt);
        this.userId = userId;
        this.headline = headline;
        this.summary = summary;
        this.location = location;
        this.primarySkills = primarySkills;
    }

    public UUID getUserId() { return userId; }
    public String getHeadline() { return headline; }
    public String getSummary() { return summary; }
    public String getLocation() { return location; }
    public List<String> getPrimarySkills() { return primarySkills; }
}
