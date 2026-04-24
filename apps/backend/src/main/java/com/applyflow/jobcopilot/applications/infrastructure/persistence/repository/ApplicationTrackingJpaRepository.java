package com.applyflow.jobcopilot.applications.infrastructure.persistence.repository;

import com.applyflow.jobcopilot.applications.infrastructure.persistence.entity.ApplicationTrackingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationTrackingJpaRepository extends JpaRepository<ApplicationTrackingJpaEntity, UUID> {
    List<ApplicationTrackingJpaEntity> findByApplicationDraftIdOrderByCreatedAtAsc(UUID applicationDraftId);
}
