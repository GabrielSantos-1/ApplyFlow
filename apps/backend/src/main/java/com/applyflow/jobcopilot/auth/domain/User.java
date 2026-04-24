package com.applyflow.jobcopilot.auth.domain;

import com.applyflow.jobcopilot.shared.domain.BaseEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public class User extends BaseEntity {
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final boolean active;

    public User(UUID id, OffsetDateTime createdAt, OffsetDateTime updatedAt, String email, String passwordHash, UserRole role, boolean active) {
        super(id, createdAt, updatedAt);
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return active; }
}
