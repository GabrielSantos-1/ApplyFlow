package com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_job_search_preferences")
public class UserJobSearchPreferenceJpaEntity {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(nullable = false, length = 80)
    private String keyword;
    @Column(name = "normalized_keyword", nullable = false, length = 80)
    private String normalizedKeyword;
    @Column(length = 80)
    private String location;
    @Column(name = "remote_only", nullable = false)
    private boolean remoteOnly;
    @Column(length = 30)
    private String seniority;
    @Column(nullable = false, length = 20)
    private String provider;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Column(name = "last_run_at")
    private OffsetDateTime lastRunAt;
    @Column(name = "last_run_status", length = 20)
    private String lastRunStatus;
    @Column(name = "last_fetched_count", nullable = false)
    private int lastFetchedCount;
    @Column(name = "last_inserted_count", nullable = false)
    private int lastInsertedCount;
    @Column(name = "last_updated_count", nullable = false)
    private int lastUpdatedCount;
    @Column(name = "last_skipped_count", nullable = false)
    private int lastSkippedCount;
    @Column(name = "last_failed_count", nullable = false)
    private int lastFailedCount;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getNormalizedKeyword() { return normalizedKeyword; }
    public void setNormalizedKeyword(String normalizedKeyword) { this.normalizedKeyword = normalizedKeyword; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public boolean isRemoteOnly() { return remoteOnly; }
    public void setRemoteOnly(boolean remoteOnly) { this.remoteOnly = remoteOnly; }
    public String getSeniority() { return seniority; }
    public void setSeniority(String seniority) { this.seniority = seniority; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(OffsetDateTime lastRunAt) { this.lastRunAt = lastRunAt; }
    public String getLastRunStatus() { return lastRunStatus; }
    public void setLastRunStatus(String lastRunStatus) { this.lastRunStatus = lastRunStatus; }
    public int getLastFetchedCount() { return lastFetchedCount; }
    public void setLastFetchedCount(int lastFetchedCount) { this.lastFetchedCount = lastFetchedCount; }
    public int getLastInsertedCount() { return lastInsertedCount; }
    public void setLastInsertedCount(int lastInsertedCount) { this.lastInsertedCount = lastInsertedCount; }
    public int getLastUpdatedCount() { return lastUpdatedCount; }
    public void setLastUpdatedCount(int lastUpdatedCount) { this.lastUpdatedCount = lastUpdatedCount; }
    public int getLastSkippedCount() { return lastSkippedCount; }
    public void setLastSkippedCount(int lastSkippedCount) { this.lastSkippedCount = lastSkippedCount; }
    public int getLastFailedCount() { return lastFailedCount; }
    public void setLastFailedCount(int lastFailedCount) { this.lastFailedCount = lastFailedCount; }
}
