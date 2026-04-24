package com.applyflow.jobcopilot.vacancies.domain;

import com.applyflow.jobcopilot.shared.domain.BaseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class Vacancy extends BaseEntity {
    private final UUID platformId;
    private final String externalId;
    private final String title;
    private final String company;
    private final String location;
    private final boolean remote;
    private final String seniority;
    private final VacancyStatus status;
    private final List<String> requiredSkills;

    public Vacancy(UUID id, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID platformId, String externalId, String title,
                   String company, String location, boolean remote, String seniority, VacancyStatus status, List<String> requiredSkills) {
        super(id, createdAt, updatedAt);
        this.platformId = platformId;
        this.externalId = externalId;
        this.title = title;
        this.company = company;
        this.location = location;
        this.remote = remote;
        this.seniority = seniority;
        this.status = status;
        this.requiredSkills = requiredSkills;
    }

    public UUID getPlatformId() { return platformId; }
    public String getExternalId() { return externalId; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getLocation() { return location; }
    public boolean isRemote() { return remote; }
    public String getSeniority() { return seniority; }
    public VacancyStatus getStatus() { return status; }
    public List<String> getRequiredSkills() { return requiredSkills; }
}
