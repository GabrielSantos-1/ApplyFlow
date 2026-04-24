package com.applyflow.jobcopilot.shared.infrastructure.persistence.repository;

import com.applyflow.jobcopilot.shared.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID> {
}