package com.applyflow.jobcopilot.vacancies.domain;

import com.applyflow.jobcopilot.shared.domain.BaseEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Platform extends BaseEntity {
    private final String name;
    private final PlatformType type;

    public Platform(UUID id, OffsetDateTime createdAt, OffsetDateTime updatedAt, String name, PlatformType type) {
        super(id, createdAt, updatedAt);
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public PlatformType getType() { return type; }
}
