package com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(name = "vacancies")
public class VacancyJpaEntity {
    @Id
    private UUID id;
    @Column(name = "platform_id")
    private UUID platformId;
    @Column(name = "external_id")
    private String externalId;
    private String title;
    @Column(name = "canonical_title")
    private String canonicalTitle;
    private String company;
    @Column(name = "canonical_company")
    private String canonicalCompany;
    private String location;
    @Column(name = "canonical_location")
    private String canonicalLocation;
    @Column(name = "is_remote")
    private boolean remote;
    private String seniority;
    @Column(name = "normalized_seniority")
    private String normalizedSeniority;
    private String status;
    @Column(name = "quality_score")
    private int qualityScore;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quality_flags", columnDefinition = "jsonb")
    private JsonNode qualityFlags;
    @Column(name = "dedupe_key")
    private String dedupeKey;
    @Column(name = "is_duplicate")
    private boolean duplicateRecord;
    @Column(name = "canonical_vacancy_id")
    private UUID canonicalVacancyId;
    @Column(name = "required_skills")
    private String requiredSkills;
    @Column(name = "source_url")
    private String sourceUrl;
    @Column(name = "source")
    private String source;
    @Column(name = "source_tenant")
    private String sourceTenant;
    @Column(name = "external_job_id")
    private String externalJobId;
    @Column(name = "checksum")
    private String checksum;
    @Column(name = "source_checksum")
    private String sourceChecksum;
    @Column(name = "raw_description")
    private String rawDescription;
    @Column(name = "requirements")
    private String requirements;
    @Column(name = "remote_type")
    private String remoteType;
    @Column(name = "employment_type")
    private String employmentType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "normalized_payload", columnDefinition = "jsonb")
    private JsonNode normalizedPayload;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private JsonNode rawPayload;
    @Column(name = "discovered_at")
    private OffsetDateTime discoveredAt;
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;
    @Column(name = "last_ingested_at")
    private OffsetDateTime lastIngestedAt;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPlatformId() { return platformId; }
    public void setPlatformId(UUID platformId) { this.platformId = platformId; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCanonicalTitle() { return canonicalTitle; }
    public void setCanonicalTitle(String canonicalTitle) { this.canonicalTitle = canonicalTitle; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getCanonicalCompany() { return canonicalCompany; }
    public void setCanonicalCompany(String canonicalCompany) { this.canonicalCompany = canonicalCompany; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCanonicalLocation() { return canonicalLocation; }
    public void setCanonicalLocation(String canonicalLocation) { this.canonicalLocation = canonicalLocation; }
    public boolean isRemote() { return remote; }
    public void setRemote(boolean remote) { this.remote = remote; }
    public String getSeniority() { return seniority; }
    public void setSeniority(String seniority) { this.seniority = seniority; }
    public String getNormalizedSeniority() { return normalizedSeniority; }
    public void setNormalizedSeniority(String normalizedSeniority) { this.normalizedSeniority = normalizedSeniority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getQualityScore() { return qualityScore; }
    public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }
    public JsonNode getQualityFlags() { return qualityFlags; }
    public void setQualityFlags(JsonNode qualityFlags) { this.qualityFlags = qualityFlags; }
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    public boolean isDuplicateRecord() { return duplicateRecord; }
    public void setDuplicateRecord(boolean duplicateRecord) { this.duplicateRecord = duplicateRecord; }
    public UUID getCanonicalVacancyId() { return canonicalVacancyId; }
    public void setCanonicalVacancyId(UUID canonicalVacancyId) { this.canonicalVacancyId = canonicalVacancyId; }
    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceTenant() { return sourceTenant; }
    public void setSourceTenant(String sourceTenant) { this.sourceTenant = sourceTenant; }
    public String getExternalJobId() { return externalJobId; }
    public void setExternalJobId(String externalJobId) { this.externalJobId = externalJobId; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public String getSourceChecksum() { return sourceChecksum; }
    public void setSourceChecksum(String sourceChecksum) { this.sourceChecksum = sourceChecksum; }
    public String getRawDescription() { return rawDescription; }
    public void setRawDescription(String rawDescription) { this.rawDescription = rawDescription; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public String getRemoteType() { return remoteType; }
    public void setRemoteType(String remoteType) { this.remoteType = remoteType; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public JsonNode getNormalizedPayload() { return normalizedPayload; }
    public void setNormalizedPayload(JsonNode normalizedPayload) { this.normalizedPayload = normalizedPayload; }
    public JsonNode getRawPayload() { return rawPayload; }
    public void setRawPayload(JsonNode rawPayload) { this.rawPayload = rawPayload; }
    public OffsetDateTime getDiscoveredAt() { return discoveredAt; }
    public void setDiscoveredAt(OffsetDateTime discoveredAt) { this.discoveredAt = discoveredAt; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
    public OffsetDateTime getLastIngestedAt() { return lastIngestedAt; }
    public void setLastIngestedAt(OffsetDateTime lastIngestedAt) { this.lastIngestedAt = lastIngestedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
