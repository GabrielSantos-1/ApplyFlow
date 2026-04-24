package com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "vacancy_ingestion_locks")
public class VacancyIngestionLockJpaEntity {
    @Id
    private String source;

    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public OffsetDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(OffsetDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }
}
