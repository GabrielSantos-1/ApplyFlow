package com.applyflow.jobcopilot.applications.infrastructure.persistence.repository;

import com.applyflow.jobcopilot.applications.infrastructure.persistence.entity.ApplicationDraftJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationDraftJpaRepository extends JpaRepository<ApplicationDraftJpaEntity, UUID> {
    Page<ApplicationDraftJpaEntity> findByUserId(UUID userId, Pageable pageable);
    Optional<ApplicationDraftJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    Optional<ApplicationDraftJpaEntity> findTopByUserIdAndVacancyIdOrderByCreatedAtDesc(UUID userId, UUID vacancyId);
}
